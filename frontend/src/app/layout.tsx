import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Rupaya | Split Bills Stress-Free",
  description: "Modern bill tracking and splitting application.",
};

import { ToastProvider } from "@/components/ui/Toast";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="font-sans antialiased">
        <ToastProvider>
          {children}
        </ToastProvider>
      </body>
    </html>
  );
}
