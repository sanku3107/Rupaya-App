from uuid import UUID

from fastapi import APIRouter, Depends

from app.models.groups import (
    AddMemberRequest,
    GroupCreate,
    GroupDetailOut,
    GroupMemberOut,
    GroupOut,
    GroupUpdate,
    MemberUpdate,
)

from app.models.pagination import PaginatedResponse
from app.models.users import UserOut
from app.services.auth_service import get_current_user
from app.services.group_service import GroupService

from sqlalchemy.ext.asyncio import AsyncSession
from app.db.session import get_db

router = APIRouter(prefix="/groups", tags=["Groups"])


def get_group_service(db: AsyncSession = Depends(get_db)) -> GroupService:
    return GroupService(db)


@router.post("/", response_model=GroupOut)
async def create_group(
    data: GroupCreate,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.create_group(data, current_user.id)


@router.get("/", response_model=PaginatedResponse[GroupOut])
async def get_user_groups(
    search: str | None = None,
    filter: str | None = None,
    sort_by: str = "created_at",
    order: str = "desc",
    skip: int = 0,
    limit: int = 20,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):

    """
    Get all groups for the current user with search, filter, sort, and pagination.
    """
    return await service.get_user_groups(
        user_id=current_user.id,
        search=search,
        filter=filter,
        sort_by=sort_by,
        order=order,
        skip=skip,
        limit=limit,
    )




@router.get("/{group_id}", response_model=GroupDetailOut)
async def get_group(
    group_id: UUID,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    """
    Get group details (members only).
    """
    return await service.get_group_detail(group_id, current_user.id)


@router.post("/{group_id}/members", response_model=GroupMemberOut)
async def add_member(
    group_id: UUID,
    data: AddMemberRequest,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.add_member_to_group(group_id, data, current_user.id)


@router.delete("/{group_id}/members/{member_id}")
async def remove_member(
    group_id: UUID,
    member_id: UUID,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.remove_member_from_group(group_id, member_id, current_user.id)


@router.patch("/{group_id}", response_model=GroupOut)
async def update_group(
    group_id: UUID,
    data: GroupUpdate,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.update_group(group_id, data, current_user.id)


@router.patch("/{group_id}/members/{member_id}", response_model=GroupMemberOut)
async def update_member_role(
    group_id: UUID,
    member_id: UUID,
    data: MemberUpdate,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.update_member_role(group_id, member_id, data.role, current_user.id)



@router.delete("/{group_id}")
async def delete_group(
    group_id: UUID,
    current_user: UserOut = Depends(get_current_user),
    service: GroupService = Depends(get_group_service),
):
    return await service.delete_group(group_id, current_user.id)

