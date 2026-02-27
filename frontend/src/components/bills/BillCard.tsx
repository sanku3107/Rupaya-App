"use client";

import React from "react";
import { Receipt, ArrowUpRight, ArrowDownLeft, Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { type Bill } from "@/lib/api";

interface BillCardProps {
    bill: Bill;
    currentUserId: string | undefined;
    onEdit?: (bill: Bill) => void;
    showGroup?: boolean;
}

export function BillCard({ bill, currentUserId, onEdit, showGroup = false }: BillCardProps) {
    const myShare = bill.shares?.find((s) => s.user_id === currentUserId);
    const isPayer = bill.paid_by === currentUserId;

    // Involved if you paid OR have a positive share amount
    const isInvolved = isPayer || (myShare && myShare.amount > 0);

    return (
        <div className="bg-card border border-border rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:border-primary/30 transition-all hover:bg-primary/[0.01]">
            <div className="flex items-center gap-4">
                <div className="w-10 h-10 bg-secondary/80 rounded-xl flex items-center justify-center shrink-0">
                    <Receipt className="w-5 h-5 text-muted-foreground" />
                </div>
                <div>
                    <h4 className="font-bold text-sm tracking-tight">{bill.description}</h4>
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[10px] text-muted-foreground">
                        {showGroup && bill.group && (
                            <>
                                <span className="bg-primary/5 text-primary px-1.5 py-0.5 rounded font-bold uppercase tracking-tighter">
                                    {bill.group.name}
                                </span>
                                <span>•</span>
                            </>
                        )}
                        <span>
                            Paid by{" "}
                            <span className="font-semibold text-foreground/80">
                                {isPayer ? "You" : (bill.payer?.name || bill.payer?.email || "Unknown")}
                            </span>
                        </span>
                        <span>•</span>
                        <span>{new Date(bill.created_at).toLocaleDateString()}</span>
                    </div>
                </div>
            </div>

            <div className="flex items-center justify-between sm:justify-end gap-6 border-t sm:border-t-0 pt-3 sm:pt-0 border-border/50">
                <div className="text-right">
                    {isInvolved ? (
                        <>
                            <p className={cn("text-xs font-bold flex items-center gap-1 justify-end",
                                isPayer ? "text-emerald-500" : "text-rose-500")}>
                                {isPayer ? (
                                    <>
                                        {myShare ? (
                                            bill.total_amount - myShare.amount > 0 ? (
                                                <>
                                                    <ArrowUpRight className="w-3 h-3" />
                                                    You lent ₹{(bill.total_amount - myShare.amount).toLocaleString()}
                                                </>
                                            ) : (
                                                <>
                                                    <Receipt className="w-3 h-3 text-muted-foreground/40" />
                                                    You spent ₹{myShare.amount.toLocaleString()}
                                                </>
                                            )
                                        ) : (
                                            <>
                                                <ArrowUpRight className="w-3 h-3" />
                                                You lent ₹{bill.total_amount.toLocaleString()}
                                            </>
                                        )}
                                    </>
                                ) : (
                                    <>
                                        <ArrowDownLeft className="w-3 h-3" />
                                        You owe ₹{myShare!.amount.toLocaleString()}
                                    </>
                                )}
                            </p>
                            <p className="text-[9px] text-muted-foreground uppercase font-black tracking-widest mt-0.5">
                                {isPayer ? (
                                    (myShare ?
                                        bill.shares.filter(s => s.user_id !== currentUserId).every(s => s.paid) :
                                        bill.shares.every(s => s.paid)) ? "Settled" : "Pending"
                                ) : (
                                    myShare!.paid ? "Settled" : "Pending"
                                )}
                            </p>
                        </>
                    ) : (
                        <div className="text-right py-1 px-2 bg-secondary/30 rounded-lg">
                            <p className="text-[10px] font-black text-muted-foreground uppercase tracking-wider">
                                NOT INVOLVED
                            </p>
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-3 shrink-0">
                    <div className="text-right min-w-[70px]">
                        <p className="text-base font-black">₹{bill.total_amount.toLocaleString()}</p>
                        <p className="text-[9px] text-muted-foreground font-bold uppercase tracking-widest">Total</p>
                    </div>
                    {onEdit && (
                        <Button
                            variant="ghost"
                            size="icon"
                            className="w-8 h-8 rounded-lg hover:bg-primary/10 hover:text-primary transition-colors"
                            onClick={() => onEdit(bill)}
                        >
                            <Pencil className="w-4 h-4" />
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
