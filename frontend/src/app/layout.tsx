import type { Metadata } from "next";
import { Manrope } from "next/font/google";
import "./globals.css";

const manrope = Manrope({
  variable: "--font-manrope",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "ClinicalOS EHR System",
  description: "Secure Electronic Health Record System",
};

import { QueryProvider } from "@/providers/query-provider";
import { AuthProvider } from "@/providers/auth-provider";
import { SessionProvider } from "@/providers/session-provider";
import { TopProgressBar } from "@/components/common/top-progress-bar";
import { Suspense } from "react";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${manrope.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <Suspense fallback={null}>
          <TopProgressBar />
        </Suspense>
        <QueryProvider>
          <SessionProvider>
            <AuthProvider>{children}</AuthProvider>
          </SessionProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
