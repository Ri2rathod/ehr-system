import { NextRequest, NextResponse } from "next/server";
import { backendApi } from "@/lib/backend-api";

export async function GET(request: NextRequest) {
  try {
    const accessToken = request.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Unauthorized" }, { status: 401 });
    }
    const backendResponse = await backendApi.get("/auth/me", {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      validateStatus: () => true,
    });

    const data = backendResponse.data;

    if (backendResponse.status < 200 || backendResponse.status >= 300) {
      return NextResponse.json(data, { status: backendResponse.status });
    }

    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { message: "Failed to fetch authenticated user" },
      { status: 502 }
    );
  }
}
