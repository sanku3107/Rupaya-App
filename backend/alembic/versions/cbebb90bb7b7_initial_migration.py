"""Initial_migration

Revision ID: cbebb90bb7b7
Revises: 
Create Date: 2026-01-26 00:43:19.455061

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = 'cbebb90bb7b7'
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create Enums
    sa.Enum('USER', 'ADMIN', 'SUPER_ADMIN', name='Role').create(op.get_bind(), checkfirst=True)
    sa.Enum('ADMIN', 'MEMBER', name='GroupRole').create(op.get_bind(), checkfirst=True)
    sa.Enum('EQUAL', 'EXACT', name='SplitType').create(op.get_bind(), checkfirst=True)

    # 2. Create Tables
    
    # User
    op.create_table('User',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('email', sa.String(), nullable=False),
        sa.Column('password', sa.String(), nullable=False),
        sa.Column('role', postgresql.ENUM('USER', 'ADMIN', 'SUPER_ADMIN', name='Role', create_type=False), server_default='USER', nullable=True),
        sa.Column('created_by', sa.UUID(), nullable=True),
        sa.Column('updated_by', sa.UUID(), nullable=True),
        sa.Column('deleted_by', sa.UUID(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deleted_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['deleted_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['updated_by'], ['User.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_User_created_by'), 'User', ['created_by'], unique=False)
    op.create_index(op.f('ix_User_deleted_by'), 'User', ['deleted_by'], unique=False)
    op.create_index(op.f('ix_User_email'), 'User', ['email'], unique=True)
    op.create_index(op.f('ix_User_updated_by'), 'User', ['updated_by'], unique=False)

    # Group
    op.create_table('Group',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('description', sa.String(), nullable=True),
        sa.Column('created_by', sa.UUID(), nullable=True),
        sa.Column('updated_by', sa.UUID(), nullable=True),
        sa.Column('deleted_by', sa.UUID(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deleted_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['deleted_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['updated_by'], ['User.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_Group_created_by'), 'Group', ['created_by'], unique=False)
    op.create_index(op.f('ix_Group_deleted_by'), 'Group', ['deleted_by'], unique=False)
    op.create_index(op.f('ix_Group_name'), 'Group', ['name'], unique=False)
    op.create_index(op.f('ix_Group_updated_by'), 'Group', ['updated_by'], unique=False)

    # GroupMember
    op.create_table('GroupMember',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('user_id', sa.UUID(), nullable=False),
        sa.Column('group_id', sa.UUID(), nullable=False),
        sa.Column('created_by', sa.UUID(), nullable=False),
        sa.Column('updated_by', sa.UUID(), nullable=True),
        sa.Column('deleted_by', sa.UUID(), nullable=True),
        sa.Column('role', postgresql.ENUM('ADMIN', 'MEMBER', name='GroupRole', create_type=False), server_default='MEMBER', nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deleted_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['User.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['deleted_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['group_id'], ['Group.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['updated_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['user_id'], ['User.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'group_id', name='unique_user_group')
    )
    op.create_index(op.f('ix_GroupMember_created_by'), 'GroupMember', ['created_by'], unique=False)
    op.create_index(op.f('ix_GroupMember_deleted_by'), 'GroupMember', ['deleted_by'], unique=False)
    op.create_index(op.f('ix_GroupMember_group_id'), 'GroupMember', ['group_id'], unique=False)
    op.create_index(op.f('ix_GroupMember_updated_by'), 'GroupMember', ['updated_by'], unique=False)
    op.create_index(op.f('ix_GroupMember_user_id'), 'GroupMember', ['user_id'], unique=False)

    # Bill
    op.create_table('Bill',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('group_id', sa.UUID(), nullable=False),
        sa.Column('paid_by', sa.UUID(), nullable=False),
        sa.Column('created_by', sa.UUID(), nullable=False),
        sa.Column('updated_by', sa.UUID(), nullable=True),
        sa.Column('deleted_by', sa.UUID(), nullable=True),
        sa.Column('description', sa.String(), nullable=False),
        sa.Column('total_amount', sa.Float(), nullable=False),
        sa.Column('split_type', postgresql.ENUM('EQUAL', 'EXACT', name='SplitType', create_type=False), server_default='EQUAL', nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deleted_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['created_by'], ['User.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['deleted_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['group_id'], ['Group.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['paid_by'], ['User.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['updated_by'], ['User.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_Bill_created_by'), 'Bill', ['created_by'], unique=False)
    op.create_index(op.f('ix_Bill_deleted_by'), 'Bill', ['deleted_by'], unique=False)
    op.create_index(op.f('ix_Bill_group_id'), 'Bill', ['group_id'], unique=False)
    op.create_index(op.f('ix_Bill_paid_by'), 'Bill', ['paid_by'], unique=False)
    op.create_index(op.f('ix_Bill_updated_by'), 'Bill', ['updated_by'], unique=False)

    # BillShare
    op.create_table('BillShare',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('bill_id', sa.UUID(), nullable=False),
        sa.Column('user_id', sa.UUID(), nullable=False),
        sa.Column('created_by', sa.UUID(), nullable=True),
        sa.Column('updated_by', sa.UUID(), nullable=True),
        sa.Column('deleted_by', sa.UUID(), nullable=True),
        sa.Column('amount', sa.Float(), nullable=False),
        sa.Column('paid', sa.Boolean(), server_default=sa.text('false'), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('deleted_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['bill_id'], ['Bill.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['created_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['deleted_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['updated_by'], ['User.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['user_id'], ['User.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('bill_id', 'user_id', name='unique_bill_user')
    )
    op.create_index(op.f('ix_BillShare_bill_id'), 'BillShare', ['bill_id'], unique=False)
    op.create_index(op.f('ix_BillShare_created_by'), 'BillShare', ['created_by'], unique=False)
    op.create_index(op.f('ix_BillShare_deleted_by'), 'BillShare', ['deleted_by'], unique=False)
    op.create_index(op.f('ix_BillShare_updated_by'), 'BillShare', ['updated_by'], unique=False)
    op.create_index(op.f('ix_BillShare_user_id'), 'BillShare', ['user_id'], unique=False)


def downgrade() -> None:
    op.drop_table('BillShare')
    op.drop_table('Bill')
    op.drop_table('GroupMember')
    op.drop_table('Group')
    op.drop_table('User')
    sa.Enum(name='SplitType').drop(op.get_bind(), checkfirst=True)
    sa.Enum(name='GroupRole').drop(op.get_bind(), checkfirst=True)
    sa.Enum(name='Role').drop(op.get_bind(), checkfirst=True)
