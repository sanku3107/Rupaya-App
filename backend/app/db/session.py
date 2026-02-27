from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import sessionmaker
import os
from dotenv import load_dotenv

from sqlalchemy.engine.url import make_url

load_dotenv()

db_url_str = os.getenv("DATABASE_URL")
DATABASE_URL = ""

if db_url_str:
    url_obj = make_url(db_url_str)
    if url_obj.drivername == "postgresql":
        url_obj = url_obj.set(drivername="postgresql+asyncpg")
    
    if "schema" in url_obj.query:
        query_dict = dict(url_obj.query)
        del query_dict["schema"]
        url_obj = url_obj.set(query=query_dict)
    
    DATABASE_URL = url_obj.render_as_string(hide_password=False)

engine = create_async_engine(DATABASE_URL, echo=True)
AsyncSessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
