"use client";

import * as React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X } from "lucide-react";


interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    description?: string;
    children: React.ReactNode;
}

export function Modal({ isOpen, onClose, title, description, children }: ModalProps) {
    React.useEffect(() => {
        if (isOpen) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = 'unset';
        }
        return () => { document.body.style.overflow = 'unset'; };
    }, [isOpen]);

    return (
        <AnimatePresence>
            {isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                    />
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 10 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 10 }}
                        className="relative w-full max-w-lg bg-card border border-border shadow-2xl rounded-2xl overflow-hidden flex flex-col max-h-[85vh]"
                    >
                        <div className="p-6 border-b border-border flex items-center justify-between shrink-0">
                            <h2 className="text-xl font-bold tracking-tight">{title}</h2>
                            <button
                                onClick={onClose}
                                className="rounded-full p-1 hover:bg-secondary transition-colors"
                            >
                                <X className="w-5 h-5 text-muted-foreground" />
                            </button>
                        </div>
                        
                        <div className="p-6 overflow-y-auto">
                            {description && (
                                <p className="text-sm text-muted-foreground mb-6">{description}</p>
                            )}
                            <div className="mt-4">{children}</div>
                        </div>
                    </motion.div>
                </div>
            )}
        </AnimatePresence>
    );
}
