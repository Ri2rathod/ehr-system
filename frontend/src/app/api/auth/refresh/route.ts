import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import axios from "axios";

const API_URL = process.env.API_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get("refreshToken")?.value;

  if (!refreshToken) {
    return NextResponse.json(
      { code: "SESSION_EXPIRED", message: "Refresh token missing" },
      { status: 401 }
    );
  }

  try {
    const backendResponse = await axios.post(
      `${API_URL}/auth/refresh`,
      { refreshToken },
      {
        headers: { "Content-Type": "application/json" },
        validateStatus: () => true,
      }
    );

    const data = backendResponse.data;
    if (backendResponse.status < 200 || backendResponse.status >= 300) {
      return NextResponse.json(
        {
          code: data?.code || "SESSION_EXPIRED",
          message: data?.message || "Session expired",
        },
        { status: backendResponse.status }
      );
    }

    if (!data?.accessToken) {
      return NextResponse.json(
        { code: "SESSION_EXPIRED", message: "Invalid refresh response" },
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

    if (data.refreshToken) {
      cookieStore.set("refreshToken", data.refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "strict",
        path: "/",
        maxAge: 60 * 60 * 24 * 7,
      });
    }

    return NextResponse.json({ success: true });
  } catch {
    return NextResponse.json(
      { code: "SESSION_EXPIRED", message: "Refresh failed" },
      { status: 401 }
    );
  }
}
