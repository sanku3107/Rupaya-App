"use client";

import React from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { Home, Search, Ghost } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function NotFound() {
    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-6 bg-[radial-gradient(circle_at_top_right,var(--primary-foreground)_0%,transparent_40%)]">
            <motion.div
                initial={{ opacity: 0, scale: 0.9, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                className="max-w-md w-full text-center space-y-8"
            >
                <div className="relative inline-block">
                    <motion.div
                        animate={{
                            y: [0, -10, 0],
                            rotateZ: [0, -5, 5, 0]
                        }}
                        transition={{
                            duration: 4,
                            repeat: Infinity,
                            ease: "easeInOut"
                        }}
                        className="w-24 h-24 bg-primary/10 rounded-3xl flex items-center justify-center mx-auto"
                    >
                        <Ghost className="w-12 h-12 text-primary" />
                    </motion.div>
                    <div className="absolute -bottom-2 -right-2 bg-emerald-500 text-white text-[10px] font-black px-2 py-1 rounded-full uppercase tracking-widest shadow-lg">
                        404 Lost
                    </div>
                </div>

                <div className="space-y-3">
                    <h1 className="text-4xl font-black tracking-tight">Rupee <span className="text-primary">Strayed?</span></h1>
                    <p className="text-muted-foreground font-medium leading-relaxed">
                        The page you&apos;re looking for has vanished into thin air. Don&apos;t worry, your wallet is still safe!
                    </p>
                </div>

                <div className="flex flex-col sm:flex-row gap-3 pt-4">
                    <Link href="/dashboard" className="flex-1">
                        <Button className="w-full rounded-2xl h-12 text-sm font-bold shadow-xl shadow-primary/20">
                            <Home className="mr-2 w-4 h-4" />
                            Back to Dashboard
                        </Button>
                    </Link>
                    <Link href="/groups" className="flex-1">
                        <Button variant="outline" className="w-full rounded-2xl h-12 text-sm font-bold bg-transparent">
                            <Search className="mr-2 w-4 h-4" />
                            Browse Groups
                        </Button>
                    </Link>
                </div>

                <p className="text-[10px] text-muted-foreground uppercase font-black tracking-[0.3em] pt-8 opacity-50">
                    Rupaya Engine • Error Boundary
                </p>
            </motion.div>
        </div>
    );
}
