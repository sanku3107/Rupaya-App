from fastapi import APIRouter, Depends
from fastapi.security import OAuth2PasswordRequestForm

from app.core.security import oauth2_scheme
from app.models.auth import RefreshTokenRequest
from app.services.auth_service import AuthService

from sqlalchemy.ext.asyncio import AsyncSession
from app.db.session import get_db

router = APIRouter(prefix="/auth", tags=["Auth"])


def get_auth_service(db: AsyncSession = Depends(get_db)) -> AuthService:
    return AuthService(db)


@router.post("/login")
async def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    service: AuthService = Depends(get_auth_service),
):
    return await service.login_user(form_data.username, form_data.password)


@router.post("/logout")
async def logout(
    token: str = Depends(oauth2_scheme),
    service: AuthService = Depends(get_auth_service),
):
    return await service.logout_user(token)


@router.post("/logout-all")
async def logout_all(
    token: str = Depends(oauth2_scheme),
    service: AuthService = Depends(get_auth_service),
):
    return await service.logout_all_sessions(token)


@router.post("/refresh")
async def refresh(
    data: RefreshTokenRequest,
    service: AuthService = Depends(get_auth_service),
):
    return await service.refresh_access_token(data.refresh_token)
