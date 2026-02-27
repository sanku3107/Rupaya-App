from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.exceptions import (
    ConflictError,
    NotFoundError,
    ValidationError,
)
from app.core.security import hash_password, verify_password
from app.db.models import User


class UserService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def register_user(self, name: str, email: str, password: str):
        if len(password) < 8:
            raise ValidationError("Password must be at least 8 characters")

        result = await self.db.execute(select(User).where(User.email == email))
        existing = result.scalar_one_or_none()
        if existing:
            raise ConflictError("Email already registered")

        hashed_pw = hash_password(password)
        new_user = User(name=name, email=email, password=hashed_pw)
        self.db.add(new_user)
        await self.db.commit()
        await self.db.refresh(new_user)
        return new_user

    async def change_user_password(self, user_id: str, old_password: str, new_password: str):
        if len(new_password) < 8:
            raise ValidationError("New password must be at least 8 characters")

        result = await self.db.execute(select(User).where(User.id == user_id))
        user = result.scalar_one_or_none()
        if not user:
            raise NotFoundError("User not found")

        if not verify_password(old_password, user.password):
            raise ValidationError("Old password incorrect")

        hashed_new = hash_password(new_password)
        user.password = hashed_new
        await self.db.commit()
        return {"detail": "Password changed successfully"}

    async def get_user_by_id(self, user_id: str):
        result = await self.db.execute(select(User).where(User.id == user_id))
        user = result.scalar_one_or_none()
        if not user:
            raise NotFoundError("User not found")
        return user

    async def search_users(self, query: str, current_user_id: str):
        """Search users by name or email, excluding the current user."""
        stmt = (
            select(User)
            .where(
                User.id != current_user_id,
                or_(
                    User.name.ilike(f"%{query}%"),
                    User.email.ilike(f"%{query}%")
                )
            )
            .limit(10)
        )
        result = await self.db.execute(stmt)
        return result.scalars().all()

