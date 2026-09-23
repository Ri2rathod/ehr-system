"use client";

import { useEffect, useState, useRef } from "react";
import { usePathname, useSearchParams } from "next/navigation";

export function TopProgressBar() {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const isFirstRender = useRef(true);

  // When route finishes changing
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    // Complete the progress to 100% from current state, then hide and reset to 0
    setLoading(true);
    setProgress(100);

    const finishTimer = setTimeout(() => {
      setLoading(false);
      setTimeout(() => {
        setProgress(0);
      }, 200);
    }, 300);

    return () => clearTimeout(finishTimer);
  }, [pathname, searchParams]);

  // Intercept clicks on internal links to reset progress to 0 and animate left-to-right
  useEffect(() => {
    let timer1: NodeJS.Timeout;
    let timer2: NodeJS.Timeout;
    let timer3: NodeJS.Timeout;

    const startAnimation = () => {
      setProgress(0);
      setLoading(true);

      // Frame 1: move from 0 to 25%
      timer1 = setTimeout(() => setProgress(25), 50);
      // Frame 2: move to 60%
      timer2 = setTimeout(() => setProgress(60), 200);
      // Frame 3: move to 85% while waiting for page load
      timer3 = setTimeout(() => setProgress(85), 500);
    };

    const handleAnchorClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement | null;
      const anchor = target?.closest("a");

      if (anchor && anchor.href && anchor.target !== "_blank") {
        const currentUrl = new URL(window.location.href);
        const targetUrl = new URL(anchor.href, window.location.href);

        if (
          targetUrl.origin === currentUrl.origin &&
          (targetUrl.pathname !== currentUrl.pathname ||
            targetUrl.search !== currentUrl.search)
        ) {
          clearTimeout(timer1);
          clearTimeout(timer2);
          clearTimeout(timer3);
          startAnimation();
        }
      }
    };

    document.addEventListener("click", handleAnchorClick);
    return () => {
      document.removeEventListener("click", handleAnchorClick);
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
    };
  }, []);

  if (!loading && progress === 0) return null;

  return (
    <div
      className="fixed top-0 left-0 right-0 z-[99999] pointer-events-none h-1 bg-transparent"
      style={{
        opacity: loading ? 1 : 0,
        transition: "opacity 0.2s ease-in-out",
      }}
    >
      <div
        className="h-full bg-[var(--color-primary,#00478d)] shadow-[0_0_10px_var(--color-primary,#00478d),0_0_5px_var(--color-primary,#00478d)] transition-all duration-300 ease-out"
        style={{
          width: `${progress}%`,
        }}
      />
    </div>
  );
}
