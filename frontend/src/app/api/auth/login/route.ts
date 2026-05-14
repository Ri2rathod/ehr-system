import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import axios from "axios";

const API_URL = process.env.API_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    console.log(`${API_URL}/auth/me`);

    const backendResponse = await axios.post(`${API_URL}/auth/login`, body, {
      headers: {
        "Content-Type": "application/json",
      },
      validateStatus: () => true,
    });

    const data = backendResponse.data;

    if (backendResponse.status < 200 || backendResponse.status >= 300) {
      return NextResponse.json(
        {
          success: false,
          message: data?.message || "Login failed",
        },
        { status: backendResponse.status }
      );
    }

    if (!data || !data.accessToken || !data.refreshToken) {
      return NextResponse.json(
        {
          success: false,
          message: "Invalid auth response from backend",
        },
        { status: 502 }
      );
    }

    const cookieStore = await cookies();

    cookieStore.set("accessToken", data.accessToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 15,
    });

    cookieStore.set("refreshToken", data.refreshToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "strict",
      path: "/",
      maxAge: 60 * 60 * 24 * 7,
    });

    const user = data.user ?? {
      userId: data.userId,
      userUuid: data.userUuid,
      email: data.email,
      username: data.username,
      firstName: data.firstName,
      lastName: data.lastName,
      displayName: data.displayName,
      roles: data.roles,
      lastLoginAt: data.lastLoginAt,
    };

    return NextResponse.json({ success: true, user });
  } catch(error) {
    console.error(error);
    return NextResponse.json(
      { success: false, message: "Internal server error" },
      { status: 500 }
    );
  }
}
