"use client";

import React, { useState, useCallback, createContext, useContext } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, AlertCircle, Info, X } from "lucide-react";
import { cn } from "@/lib/utils";

type ToastType = "success" | "error" | "info";

interface Toast {
    id: string;
    message: string;
    type: ToastType;
}

interface ToastContextType {
    toast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
    const [toasts, setToasts] = useState<Toast[]>([]);

    const toast = useCallback((message: string, type: ToastType = "info") => {
        const id = Math.random().toString(36).substring(2, 9);
        setToasts((prev) => [...prev, { id, message, type }]);

        // Auto-remove after 4 seconds
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id));
        }, 4000);
    }, []);

    const removeToast = (id: string) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    };

    return (
        <ToastContext.Provider value={{ toast }}>
            {children}
            <div className="fixed bottom-6 right-6 z-[100] flex flex-col gap-3 pointer-events-none">
                <AnimatePresence mode="popLayout">
                    {toasts.map((t) => (
                        <motion.div
                            key={t.id}
                            layout
                            initial={{ opacity: 0, x: 50, scale: 0.8, rotateZ: 5 }}
                            animate={{ opacity: 1, x: 0, scale: 1, rotateZ: 0 }}
                            exit={{ opacity: 0, x: 20, scale: 0.8, transition: { duration: 0.2 } }}
                            className={cn(
                                "pointer-events-auto min-w-[300px] max-w-md p-4 rounded-2xl shadow-2xl flex items-center gap-3 border backdrop-blur-md",
                                t.type === "success" && "bg-emerald-500/10 border-emerald-500/20 text-emerald-500",
                                t.type === "error" && "bg-rose-500/10 border-rose-500/20 text-rose-500",
                                t.type === "info" && "bg-blue-500/10 border-blue-500/20 text-blue-500"
                            )}
                        >
                            <div className="shrink-0">
                                {t.type === "success" && <CheckCircle2 className="w-5 h-5" />}
                                {t.type === "error" && <AlertCircle className="w-5 h-5" />}
                                {t.type === "info" && <Info className="w-5 h-5" />}
                            </div>
                            <p className="text-sm font-bold flex-1 leading-snug">{t.message}</p>
                            <button
                                onClick={() => removeToast(t.id)}
                                className="shrink-0 p-1 hover:bg-black/5 rounded-lg transition-colors"
                            >
                                <X className="w-4 h-4" />
                            </button>
                        </motion.div>
                    ))}
                </AnimatePresence>
            </div>
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);
    if (context === undefined) {
        throw new Error("useToast must be used within a ToastProvider");
    }
    return context;
}
