import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import axios from "axios";

const API_URL = process.env.API_URL || "http://localhost:8080";

export async function POST(request: NextRequest) {
  const accessToken = request.cookies.get("accessToken")?.value;
  const refreshToken = request.cookies.get("refreshToken")?.value;

  try {
    await axios.post(
      `${API_URL}/auth/logout`,
      {
        refreshToken,
      },
      {
        headers: accessToken
          ? {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "application/json",
            }
          : {
              "Content-Type": "application/json",
            },
        validateStatus: () => true,
      }
    );
  } catch {
    // Continue with local cookie cleanup even if backend logout fails.
  }

  const cookieStore = await cookies();
  cookieStore.set("accessToken", "", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 0,
  });
  cookieStore.set("refreshToken", "", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: 0,
  });

  return NextResponse.json({ success: true });
}
