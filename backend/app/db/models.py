import uuid
from datetime import datetime
from enum import Enum
from sqlalchemy import Column, String, Boolean, Float, DateTime, ForeignKey, Enum as SAEnum, text, Table, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from .base import Base

class Role(str, Enum):
    USER = "USER"
    ADMIN = "ADMIN"
    SUPER_ADMIN = "SUPER_ADMIN"

class GroupRole(str, Enum):
    ADMIN = "ADMIN"
    MEMBER = "MEMBER"

class SplitType(str, Enum):
    EQUAL = "EQUAL"
    EXACT = "EXACT"

class User(Base):
    __tablename__ = "User"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    name = Column(String, nullable=False)
    email = Column(String, unique=True, nullable=False, index=True)
    password = Column(String, nullable=False)
    role = Column(SAEnum(Role, name="Role"), default=Role.USER)
    
    created_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    updated_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    deleted_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    
    created_at = Column(DateTime(timezone=True), server_default=text("now()"), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

    # Relations
    # Self-referential audit relations
    # We might need to use string for late binding or lambdas if the class isn't fully defined yet
    # Since we are inside the class, we can use strings.
    
    # Relationships for groups
    group_memberships = relationship("GroupMember", back_populates="user", foreign_keys="GroupMember.user_id")
    
    # Simplify for now, add backrefs as needed in other models
    
class Group(Base):
    __tablename__ = "Group"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    name = Column(String, nullable=False, index=True)
    description = Column(String, nullable=True)
    
    created_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    updated_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    deleted_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    
    created_at = Column(DateTime(timezone=True), server_default=text("now()"), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

    members = relationship("GroupMember", back_populates="group")
    bills = relationship("Bill", back_populates="group")

class GroupMember(Base):
    __tablename__ = "GroupMember"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    user_id = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="CASCADE"), nullable=False, index=True)
    group_id = Column(UUID(as_uuid=True), ForeignKey("Group.id", ondelete="CASCADE"), nullable=False, index=True)
    
    created_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="RESTRICT"), nullable=False, index=True)
    updated_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    deleted_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    
    role = Column(SAEnum(GroupRole, name="GroupRole"), default=GroupRole.MEMBER)
    
    created_at = Column(DateTime(timezone=True), server_default=text("now()"), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

    user = relationship("User", back_populates="group_memberships", foreign_keys=[user_id])
    group = relationship("Group", back_populates="members")
    
    __table_args__ = (
        UniqueConstraint('user_id', 'group_id', name='unique_user_group'),
    )

class Bill(Base):
    __tablename__ = "Bill"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    group_id = Column(UUID(as_uuid=True), ForeignKey("Group.id", ondelete="CASCADE"), nullable=False, index=True)
    paid_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="RESTRICT"), nullable=False, index=True)
    created_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="RESTRICT"), nullable=False, index=True)
    updated_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    deleted_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    
    description = Column(String, nullable=False)
    total_amount = Column(Float, nullable=False)
    split_type = Column(SAEnum(SplitType, name="SplitType"), default=SplitType.EQUAL)
    
    created_at = Column(DateTime(timezone=True), server_default=text("now()"), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

    group = relationship("Group", back_populates="bills")
    payer = relationship("User", foreign_keys=[paid_by])
    shares = relationship("BillShare", back_populates="bill")

class BillShare(Base):
    __tablename__ = "BillShare"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()"))
    bill_id = Column(UUID(as_uuid=True), ForeignKey("Bill.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="CASCADE"), nullable=False, index=True)
    
    created_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    updated_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    deleted_by = Column(UUID(as_uuid=True), ForeignKey("User.id", ondelete="SET NULL"), nullable=True, index=True)
    
    amount = Column(Float, nullable=False)
    paid = Column(Boolean, default=False)
    
    created_at = Column(DateTime(timezone=True), server_default=text("now()"), nullable=False)
    updated_at = Column(DateTime(timezone=True), nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

    bill = relationship("Bill", back_populates="shares")
    user = relationship("User", foreign_keys=[user_id])

    __table_args__ = (
        UniqueConstraint('bill_id', 'user_id', name='unique_bill_user'),
    )
