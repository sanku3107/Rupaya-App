"use client";

import React from "react";
import { XCircle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

interface ErrorBoxProps {
    error: string | null;
    className?: string;
}

export function ErrorBox({ error, className }: ErrorBoxProps) {
    return (
        <AnimatePresence>
            {error && (
                <motion.div
                    initial={{ opacity: 0, scale: 0.95, y: -10 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95, y: -10 }}
                    className={cn(
                        "bg-rose-500/10 border border-rose-500/20 rounded-xl p-4 flex items-start gap-3",
                        className
                    )}
                >
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div className="flex-1">
                        <h4 className="text-sm font-bold text-rose-500">Error Occurred</h4>
                        <p className="text-xs text-rose-500/80 font-medium leading-relaxed mt-1">
                            {error}
                        </p>
                    </div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
