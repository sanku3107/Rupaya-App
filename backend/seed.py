import asyncio
import logging
from datetime import datetime

from sqlalchemy import select

from app.db.session import AsyncSessionLocal
from app.db.models import User, Group, GroupMember, Bill, BillShare, GroupRole, SplitType
from app.core.security import hash_password

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def seed_db():
    async with AsyncSessionLocal() as session:
        logger.info("Checking for existing users...")
        result = await session.execute(select(User))
        existing_user = result.scalars().first()

        if existing_user:
            logger.info("Users already exist. Skipping seeding.")
            return

        logger.info("No users found. Seeding initial data...")
        
        # 1. Create Users
        users = []
        user_names = ["Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace"]
        
        default_password = hash_password("password")
        
        for i, name in enumerate(user_names):
            user = User(
                name=name,
                email=f"{name.lower()}@example.com",
                password=default_password # They all share the same password for easy testing
            )
            users.append(user)
            session.add(user)
        
        # Also ensure our simple admin exists if someone wants that specific one
        admin_user = User(
            name="Admin User",
            email="admin@admin.com",
            password=hash_password("admin")
        )
        users.insert(0, admin_user) # Add to start
        session.add(admin_user)
        
        await session.flush() # Flush to get IDs
        
        logger.info(f"Created {len(users)} users.")

        # 2. Create Group
        # Alice (users[1]) creates the group
        creator = users[1] 
        group = Group(
            name="Goa Trip 2024",
            description="Best trip ever! 🌴🌊",
            created_by=creator.id
        )
        session.add(group)
        await session.flush()
        
        # 3. Add Members
        # Add everyone to the group
        for user in users:
            role = GroupRole.ADMIN if user.id == creator.id or user.email == "admin@admin.com" else GroupRole.MEMBER
            member = GroupMember(
                user_id=user.id,
                group_id=group.id,
                role=role,
                created_by=creator.id
            )
            session.add(member)
        
        logger.info(f"Created group '{group.name}' with {len(users)} members.")

        # 4. Create Bills
        bills_data = [
            {
                "desc": "Flight Tickets",
                "amount": 35000.0,
                "payer": users[1], # Alice
                "split": SplitType.EQUAL
            },
            {
                "desc": "Villa Booking",
                "amount": 70000.0,
                "payer": users[2], # Bob
                "split": SplitType.EQUAL
            },
             {
                "desc": "Dinner at Thalassa",
                "amount": 8000.0,
                "payer": users[3], # Charlie
                "split": SplitType.EQUAL
            },
            {
                "desc": "Drinks & Snacks",
                "amount": 2500.0,
                "payer": users[4], # David
                "split": SplitType.EQUAL
            }
        ]

        for b in bills_data:
            bill = Bill(
                group_id=group.id,
                paid_by=b["payer"].id,
                created_by=b["payer"].id,
                description=b["desc"],
                total_amount=b["amount"],
                split_type=b["split"]
            )
            session.add(bill)
            await session.flush()

            # Create shares
            # Equal split: everyone present
            share_amount = round(b["amount"] / len(users), 2)
            
            # Helper to handle rounding difference
            total_share = 0
            
            for i, user in enumerate(users):
                # Adjust last person's share to account for rounding errors
                if i == len(users) - 1:
                    this_share = round(b["amount"] - total_share, 2)
                else:
                    this_share = share_amount
                    total_share += this_share

                is_payer = (user.id == b["payer"].id)
                
                share = BillShare(
                    bill_id=bill.id,
                    user_id=user.id,
                    amount=this_share,
                    paid=is_payer, # Payer has theoretically "paid" their own share in the context of settlement calculations usually, but data model might imply 'paid' means settled. 
                    # Actually typically in Splitwise models:
                    # Payer pays TOTAL.
                    # Shares represent WHO OWES.
                    # 'paid' boolean usually tracks if the DEBT is settled.
                    # So initially, non-payers have paid=False. Payer's share is implicitly "covered".
                    # Let's verify standard boolean usage: usually means "Settled". 
                    # So everyone is False effectively, unless we consider the payer's self-share "settled". 
                    # For simplicity let's assume False for debts.
                    created_by=b["payer"].id
                )
                session.add(share)
        
        await session.commit()
        
        logger.info("Seeding complete! 🚀")

if __name__ == "__main__":
    asyncio.run(seed_db())
