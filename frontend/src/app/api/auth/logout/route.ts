import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendApi } from "@/lib/backend-api";

export async function POST(request: NextRequest) {
  const accessToken = request.cookies.get("accessToken")?.value;
  const refreshToken = request.cookies.get("refreshToken")?.value;

  try {
    await backendApi.post(
      "/auth/logout",
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
