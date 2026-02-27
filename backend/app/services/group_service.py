# app/services/group_service.py
from datetime import datetime
from uuid import UUID

from sqlalchemy import select, or_, and_, func
from sqlalchemy.orm import selectinload
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ForbiddenError, NotFoundError, ValidationError
from app.db.models import Group, GroupMember, User, Bill, BillShare, GroupRole
from app.models.groups import AddMemberRequest, GroupCreate, GroupUpdate, GroupDetailOut, GroupMemberOut
from app.models.users import UserOut


class GroupService:
    def __init__(self, db: AsyncSession):
        self.db = db

    # auth helper
    async def check_is_member(self, user_id: UUID | str, group_id: UUID | str):
        if isinstance(user_id, str):
            user_id = UUID(user_id)
        if isinstance(group_id, str):
            group_id = UUID(group_id)
            
        """
        Verify if a user is an active member of a group.
        Raises 403 if not a member.
        Returns the membership record if valid.
        """
        stmt = select(GroupMember).where(
            GroupMember.user_id == user_id,
            GroupMember.group_id == group_id,
            GroupMember.deleted_at.is_(None)
        )
        result = await self.db.execute(stmt)
        member = result.scalar_one_or_none()
        
        if not member:
            raise ForbiddenError("User is not a member of this group")
        return member

    async def check_is_admin(self, user_id: UUID | str, group_id: UUID | str):
        member = await self.check_is_member(user_id, group_id)
        if member.role != GroupRole.ADMIN:
            raise ForbiddenError("Only group admins can perform this action")
        return member

    async def create_group(self, data: GroupCreate, creator_id: str):
        if not data.initial_members:
            raise ValidationError("A group must have at least one other member.")

        group = Group(
            name=data.name,
            description=data.description,
            created_by=creator_id
        )
        self.db.add(group)
        await self.db.flush() # Generate ID

        # creator = admin
        creator_member = GroupMember(
            user_id=creator_id,
            group_id=group.id,
            role=GroupRole.ADMIN,
            created_by=creator_id
        )
        self.db.add(creator_member)

        added_count = 0
        for email in data.initial_members:
            result = await self.db.execute(select(User).where(User.email == email))
            user = result.scalar_one_or_none()
            
            if not user or str(user.id) == creator_id:
                continue

            member = GroupMember(
                user_id=user.id,
                group_id=group.id,
                role=GroupRole.MEMBER,
                created_by=creator_id
            )
            self.db.add(member)
            added_count += 1

        if added_count == 0:
            await self.db.rollback()
            raise ValidationError("A group must have at least one other valid member.")
        
        await self.db.commit()
        await self.db.refresh(group)
        return group

    # -------------------------
    # READ OPERATIONS
    # -------------------------
    async def get_user_groups(
        self, user_id: UUID | str, search: str | None = None, 
        filter: str | None = None,
        sort_by: str = "created_at", order: str = "desc",
        skip: int = 0, limit: int = 20
    ):
        if isinstance(user_id, str):
            user_id = UUID(user_id)
            
        # 1. Get all memberships for the user
        stmt = select(GroupMember).options(
            selectinload(GroupMember.group).selectinload(Group.members)
        ).where(
            GroupMember.user_id == user_id,
            GroupMember.deleted_at.is_(None)
        ).join(Group).where(Group.deleted_at.is_(None))

        if search:
            stmt = stmt.where(
                or_(
                    Group.name.ilike(f"%{search}%"),
                    Group.description.ilike(f"%{search}%")
                )
            )

        result = await self.db.execute(stmt)
        memberships = result.scalars().all()

        # 2. Process each group to add metrics
        groups_list = []
        for m in memberships:
            group = m.group
            if not group: 
                continue # Should not happen due to join but safety
            
            # Sub-query for Owed (others owe user in this group)
            owed_stmt = select(func.sum(BillShare.amount))\
                .join(Bill, Bill.id == BillShare.bill_id)\
                .where(
                    Bill.group_id == group.id,
                    Bill.paid_by == user_id,
                    Bill.deleted_at.is_(None),
                    BillShare.user_id != user_id,
                    BillShare.paid == False
                )
            owed_res = await self.db.execute(owed_stmt)
            total_owed = owed_res.scalar() or 0

            # Sub-query for Owe (user owes others in this group)
            owe_stmt = select(func.sum(BillShare.amount))\
                .join(Bill, Bill.id == BillShare.bill_id)\
                .where(
                    Bill.group_id == group.id,
                    Bill.paid_by != user_id,
                    Bill.deleted_at.is_(None),
                    BillShare.user_id == user_id,
                    BillShare.paid == False
                )
            owe_res = await self.db.execute(owe_stmt)
            total_owe = owe_res.scalar() or 0

            # --- FILTERING LOGIC ---
            # "owe" shows groups where you net owe money (Owe > Owed)
            if filter == "owe" and total_owe <= total_owed:
                continue
            # "owed" shows groups where you are net owed money (Owed > Owe)
            if filter == "owed" and total_owed <= total_owe:
                continue

            active_members = [mem for mem in group.members if mem.deleted_at is None]
            
            g_dict = {
                "id": str(group.id),
                "name": group.name,
                "description": group.description,
                "created_by": str(group.created_by) if group.created_by else None,
                "created_at": group.created_at,
                "updated_at": group.updated_at,
                "member_count": len(active_members),
                "total_owed": total_owed,
                "total_owe": total_owe
            }
            groups_list.append(g_dict)

        # 3. Handle Sorting
        reverse = (order.lower() == "desc")
        
        if sort_by == "name":
            groups_list.sort(key=lambda x: x["name"].lower(), reverse=reverse)
        elif sort_by == "owed":
            # Sort by net amount others owe you
            groups_list.sort(key=lambda x: x["total_owed"] - x["total_owe"], reverse=reverse)
        elif sort_by == "owe":
            # Sort by net amount you owe others
            groups_list.sort(key=lambda x: x["total_owe"] - x["total_owed"], reverse=reverse)
        else: # default: created_at
            groups_list.sort(key=lambda x: x["created_at"], reverse=reverse)

        # 4. Handle Pagination
        total = len(groups_list)
        paginated_items = groups_list[skip : skip + limit]

        return {
            "items": paginated_items,
            "total": total,
            "skip": skip,
            "limit": limit,
            "has_more": (skip + limit) < total,
        }

    async def get_group_detail(self, group_id: UUID | str, user_id: UUID | str):
        await self.check_is_member(user_id, group_id)

        stmt = select(Group).options(
            selectinload(Group.members).selectinload(GroupMember.user)
        ).where(Group.id == group_id)
        
        result = await self.db.execute(stmt)
        group = result.scalar_one_or_none()

        if not group:
            raise NotFoundError("Group not found")

        # Use Pydantic models to ensure correct serialization and validation
        members_out = []
        # Filter members
        active_members = [m for m in group.members if m.deleted_at is None]
        
        for m in active_members:
            # Construct UserOut. Role is Enum, passed directly.
            user_out = UserOut(
                id=m.user.id,
                name=m.user.name,
                email=m.user.email,
                role=m.user.role
            )
            
            members_out.append(GroupMemberOut(
                id=m.id,
                user=user_out,
                role=m.role, # Enum, will be serialized to str
                created_at=m.created_at
            ))

        # Calculate summary metrics for this group
        # Owed (others owe user in this group)
        owed_stmt = select(func.sum(BillShare.amount))\
            .join(Bill, Bill.id == BillShare.bill_id)\
            .where(
                Bill.group_id == group.id,
                Bill.paid_by == user_id,
                Bill.deleted_at.is_(None),
                BillShare.user_id != user_id,
                BillShare.paid == False
            )
        owed_res = await self.db.execute(owed_stmt)
        total_owed = owed_res.scalar() or 0

        # Owe (user owes others in this group)
        owe_stmt = select(func.sum(BillShare.amount))\
            .join(Bill, Bill.id == BillShare.bill_id)\
            .where(
                Bill.group_id == group.id,
                Bill.paid_by != user_id,
                Bill.deleted_at.is_(None),
                BillShare.user_id == user_id,
                BillShare.paid == False
            )
        owe_res = await self.db.execute(owe_stmt)
        total_owe = owe_res.scalar() or 0

        return GroupDetailOut(
            id=group.id,
            name=group.name,
            description=group.description,
            created_by=group.created_by,
            created_at=group.created_at,
            updated_at=group.updated_at,
            members=members_out,
            member_count=len(members_out),
            total_owed=total_owed,
            total_owe=total_owe
        )

    # -------------------------
    # MEMBERSHIP MANAGEMENT
    # -------------------------
    async def add_member_to_group(
        self,
        group_id: UUID | str,
        data: AddMemberRequest,
        added_by_id: UUID | str,
    ):
        await self.check_is_admin(added_by_id, group_id)

        res = await self.db.execute(select(User).where(User.email == data.email))
        user = res.scalar_one_or_none()
        if not user:
            raise NotFoundError("User not found")

        stmt = select(GroupMember).where(
            GroupMember.user_id == user.id,
            GroupMember.group_id == group_id
        )
        res = await self.db.execute(stmt)
        existing = res.scalar_one_or_none()

        if existing:
            if existing.deleted_at is None:
                raise ValidationError("User is already a member")

            # Reactivate
            existing.deleted_at = None
            existing.deleted_by = None
            existing.role = data.role
            existing.updated_by = added_by_id
            existing.updated_at = datetime.utcnow()
            await self.db.commit()
            await self.db.refresh(existing) 
            
            res = await self.db.execute(select(GroupMember).options(selectinload(GroupMember.user)).where(GroupMember.id == existing.id))
            return res.scalar_one()

        new_member = GroupMember(
            user_id=user.id,
            group_id=group_id,
            role=data.role,
            created_by=added_by_id
        )
        self.db.add(new_member)
        await self.db.commit()
        
        # Reload with user
        res = await self.db.execute(select(GroupMember).options(selectinload(GroupMember.user)).where(GroupMember.id == new_member.id))
        return res.scalar_one()

    async def remove_member_from_group(
        self,
        group_id: str,
        member_id: str,
        removed_by_id: str,
    ):
        stmt = select(GroupMember).where(GroupMember.id == member_id)
        res = await self.db.execute(stmt)
        member = res.scalar_one_or_none()
        
        if not member:
            raise NotFoundError("Member not found")

        # Allow if admin OR if removing self
        if str(member.user_id) != str(removed_by_id):
             await self.check_is_admin(removed_by_id, group_id)
        
        member.deleted_at = datetime.utcnow()
        member.deleted_by = removed_by_id
        await self.db.commit()
        return member 

    async def delete_group(self, group_id: str, user_id: str):
        """
        Soft delete a group and all its memberships.
        Requires admin privileges.
        """
        await self.check_is_admin(user_id, group_id)

        now = datetime.utcnow()

        # Update the group to mark as deleted
        stmt_group = select(Group).where(Group.id == group_id)
        res = await self.db.execute(stmt_group)
        group = res.scalar_one_or_none()
        if group:
            group.deleted_at = now
            group.deleted_by = user_id
        
        # Soft delete all memberships
        res = await self.db.execute(select(GroupMember).where(GroupMember.group_id == group_id, GroupMember.deleted_at.is_(None)))
        members = res.scalars().all()
        for m in members:
            m.deleted_at = now
            m.deleted_by = user_id

        # Soft delete all bills
        res = await self.db.execute(select(Bill).where(Bill.group_id == group_id, Bill.deleted_at.is_(None)))
        bills = res.scalars().all()
        for b in bills:
            b.deleted_at = now
            b.deleted_by = user_id

        await self.db.commit()

        return {"message": "Group deleted successfully"}

    async def update_group(self, group_id: str, data: GroupUpdate, user_id: str):
        """
        Update group details. Everyone in the group can currently do this.
        """
        await self.check_is_member(user_id, group_id)
        
        stmt = select(Group).where(Group.id == group_id)
        res = await self.db.execute(stmt)
        group = res.scalar_one_or_none()
        
        if not group:
            raise NotFoundError("Group not found")

        if data.name is not None:
            group.name = data.name
        if data.description is not None:
            group.description = data.description
            
        group.updated_at = datetime.utcnow()
        group.updated_by = user_id
        
        await self.db.commit()
        return group

    async def update_member_role(self, group_id: str, member_id: str, role: str, user_id: str):
        """
        Change a member's role. Requires admin privileges.
        """
        await self.check_is_admin(user_id, group_id)
        
        # Check if member exists in this group
        stmt = select(GroupMember).options(selectinload(GroupMember.user)).where(GroupMember.id == member_id, GroupMember.group_id == group_id)
        res = await self.db.execute(stmt)
        member = res.scalar_one_or_none()
        
        if not member:
            raise NotFoundError("Member not found in this group")
            
        member.role = role
        member.updated_at = datetime.utcnow()
        member.updated_by = user_id
        
        await self.db.commit()
        return member
