# app/services/summary_service.py
from typing import Optional
from uuid import UUID

from sqlalchemy import select, func, and_
from sqlalchemy.orm import selectinload

from app.db.models import GroupMember, Bill, BillShare, User
from app.services.group_service import GroupService


class SummaryService:
    def __init__(self, group_service: GroupService):
        self.group_service = group_service

    @property
    def db(self):
        return self.group_service.db

    async def get_user_summary(self, user_id: UUID | str, group_id: Optional[UUID | str] = None):
        if isinstance(user_id, str):
            user_id = UUID(user_id)
        if isinstance(group_id, str):
            group_id = UUID(group_id)
            
        """
        Returns summary metrics for a user.
        If group_id is provided, returns summary limited to that group.
        """
        # If group_id is provided, validate membership
        if group_id:
            await self.group_service.check_is_member(user_id, group_id)

        # 1. Total Groups (only count this group if group_id provided)
        if group_id:
            group_count = 1
        else:
            stmt = select(func.count(GroupMember.id)).where(
                GroupMember.user_id == user_id,
                GroupMember.deleted_at.is_(None)
            )
            res = await self.db.execute(stmt)
            group_count = res.scalar() or 0

        # 2. Total Owed (others owe you)
        # Bill where paid_by = user_id, share where user_id != user_id, paid = False
        stmt_owed = select(func.sum(BillShare.amount))\
            .join(Bill, Bill.id == BillShare.bill_id)\
            .where(
                Bill.paid_by == user_id,
                Bill.deleted_at.is_(None),
                BillShare.user_id != user_id,
                BillShare.paid == False
            )
        
        if group_id:
            stmt_owed = stmt_owed.where(Bill.group_id == group_id)
            
        res_owed = await self.db.execute(stmt_owed)
        total_owed = res_owed.scalar() or 0

        # 3. Total Owe (you owe others)
        # Bill where paid_by != user_id, share where user_id = user_id, paid = False
        stmt_owe = select(func.sum(BillShare.amount))\
            .join(Bill, Bill.id == BillShare.bill_id)\
            .where(
                Bill.paid_by != user_id,
                Bill.deleted_at.is_(None),
                BillShare.user_id == user_id,
                BillShare.paid == False
            )
        
        if group_id:
            stmt_owe = stmt_owe.where(Bill.group_id == group_id)
            
        res_owe = await self.db.execute(stmt_owe)
        total_owe = res_owe.scalar() or 0

        # 4. Friends (people you share groups with) - only for global summary
        friends_map = {}

        if not group_id:
            # Subquery for my groups
            my_groups_sub = select(GroupMember.group_id).where(
                GroupMember.user_id == user_id,
                GroupMember.deleted_at.is_(None)
            ).scalar_subquery()
            
            # Find members of these groups excluding self
            stmt_friends = select(User).join(GroupMember, User.id == GroupMember.user_id).where(
                GroupMember.group_id.in_(my_groups_sub),
                GroupMember.user_id != user_id,
                GroupMember.deleted_at.is_(None)
            ).distinct().limit(10)
            
            res_friends = await self.db.execute(stmt_friends)
            friends = res_friends.scalars().all()

            for friend in friends:
                friends_map[str(friend.id)] = {
                    "id": str(friend.id),
                    "name": friend.name,
                    "email": friend.email,
                }

        return {
            "total_owed": total_owed,
            "total_owe": total_owe,
            "group_count": group_count,
            "friends": list(friends_map.values())[:5],
        }
