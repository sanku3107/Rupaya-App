"use client";

import React, { useState, useEffect, useCallback } from "react";
import { Receipt, ArrowUpRight, ArrowDownLeft, Loader2, Search, Pencil } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { BillsAPI, type Bill } from "@/lib/api";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { BillCard } from "./BillCard";

interface BillListProps {
  groupId: string;
  currentUserId: string | undefined;
  onAddBill: () => void;
  onEditBill: (bill: Bill) => void;
  refreshTrigger?: number;
}

export function BillList({
  groupId,
  currentUserId,
  onAddBill,
  onEditBill,
  refreshTrigger = 0,
}: BillListProps) {

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  const fetchData = useCallback(
    async (skip: number, limit: number) => {
      return await BillsAPI.getGroupBills(groupId, skip, limit, debouncedSearch);
    },
    [groupId, debouncedSearch]
  );

  const {
    items: bills,
    loading,
    hasMore,
    loaderRef,
    reset,
  } = useInfiniteScroll<Bill>({
    fetchData,
    limit: 10,
  });

  // Reset when group, search, or refresh trigger changes
  useEffect(() => {
    reset();
  }, [groupId, debouncedSearch, refreshTrigger, reset]);

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <h2 className="text-xl font-bold">Bills</h2>

        <div className="relative w-full sm:w-64">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Search bills..."
            className="pl-9 bg-secondary/20 border-border/50 rounded-xl h-9 text-sm"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="space-y-3 max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar overscroll-contain">
        {bills.map((bill) => (
          <BillCard
            key={bill.id}
            bill={bill}
            currentUserId={currentUserId}
            onEdit={onEditBill}
          />
        ))}

        {/* Scroll Sentinel */}
        <div ref={loaderRef} className="h-4" />

        {/* Loading / Status UI */}
        <div className="py-6">
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-20 bg-secondary/10 animate-pulse rounded-2xl border border-border/20" />
              ))}
            </div>
          ) : hasMore ? (
            <div className="flex justify-center py-4 opacity-50">
              <Loader2 className="w-5 h-5 animate-spin text-primary/50" />
            </div>
          ) : bills.length === 0 ? (
            <div className="py-20 text-center border-2 border-dashed border-border rounded-3xl bg-secondary/5">
              <Receipt className="w-12 h-12 text-muted-foreground/30 mx-auto mb-4" />
              <p className="text-muted-foreground font-medium">
                {debouncedSearch ? "No balances match your search." : "No bills yet."}
              </p>
              {!debouncedSearch && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4 rounded-xl"
                  onClick={onAddBill}
                >
                  Add First Bill
                </Button>
              )}
            </div>
          ) : (
            <p className="text-center text-[10px] text-muted-foreground uppercase font-black tracking-widest py-8 opacity-40">
              You&apos;ve reached the end
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
