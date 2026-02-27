from pydantic import BaseModel, EmailStr


from enum import Enum

class Role(str, Enum):
    USER = "USER"
    ADMIN = "ADMIN"
    SUPER_ADMIN = "SUPER_ADMIN"


class UserCreate(BaseModel):
    name: str
    email: EmailStr
    password: str


class UserLogin(BaseModel):
    email: EmailStr
    password: str


from uuid import UUID

class UserOut(BaseModel):
    id: UUID
    name: str
    email: str
    role: Role

    class Config:
        from_attributes = True
