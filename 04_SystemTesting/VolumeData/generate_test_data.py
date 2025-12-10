#!/usr/bin/env python3
"""
MongoDB Volume Test Data Generator for SmartSearch
Generira velike količine testnih podatkov za volume testing
"""

import random
from datetime import datetime, timedelta
from faker import Faker
from pymongo import MongoClient
import sys

fake = Faker()

# Možne vrednosti za polja
STATUSES = ["Open", "Forthcoming", "Closed"]
SOURCES = ["ecEuropa", "cascadeFunding", "getOnePass"]
INDUSTRIES = [
    "Information Technology", "Healthcare", "Energy", "Agriculture",
    "Manufacturing", "Education", "Transportation", "Finance",
    "Biotechnology", "Telecommunications", "Aerospace", "Robotics"
]
TECHNOLOGIES = [
    "Artificial Intelligence", "Machine Learning", "Blockchain", "IoT",
    "Cloud Computing", "5G", "Quantum Computing", "Cybersecurity",
    "Big Data", "AR/VR", "Edge Computing", "Nanotechnology"
]

def generate_listing(index):
    """Generira en testni listing"""

    # Naključni datum v preteklih/bodočih 365 dneh
    days_offset = random.randint(-365, 365)
    deadline = datetime.now() + timedelta(days=days_offset)

    # Določi status glede na deadline
    if deadline < datetime.now():
        status = "Closed"
    elif deadline > datetime.now() + timedelta(days=30):
        status = "Forthcoming"
    else:
        status = "Open"

    # Generiraj listing
    listing = {
        "id": f"TEST-{index:06d}",
        "title": fake.catch_phrase() + " Funding Programme",
        "description": fake.text(max_nb_chars=500),
        "status": status,
        "source": random.choice(SOURCES),
        "deadline": deadline.strftime("%Y-%m-%d"),
        "budget": {
            "min": random.randint(10000, 100000),
            "max": random.randint(100000, 10000000),
            "currency": "EUR"
        },
        "industries": random.sample(INDUSTRIES, k=random.randint(1, 4)),
        "technologies": random.sample(TECHNOLOGIES, k=random.randint(1, 5)),
        "eligibility": fake.sentence(nb_words=15),
        "url": fake.url(),
        "createdAt": datetime.now(),
        "updatedAt": datetime.now()
    }

    return listing

def seed_database(count, mongodb_uri="mongodb://localhost:27017", db_name="smartsearch"):
    """Napolni MongoDB z testnimi podatki"""

    print(f"🚀 Starting volume data generation...")
    print(f"📊 Target: {count:,} listings")
    print(f"🔗 MongoDB URI: {mongodb_uri}")
    print(f"💾 Database: {db_name}")
    print("-" * 60)

    # Poveži se z MongoDB
    try:
        client = MongoClient(mongodb_uri)
        db = client[db_name]
        collection = db["Listings-Filtered"]

        print("✅ Connected to MongoDB")
    except Exception as e:
        print(f"❌ Failed to connect to MongoDB: {e}")
        sys.exit(1)

    # Izbriši obstoječe testne podatke
    delete_result = collection.delete_many({"id": {"$regex": "^TEST-"}})
    print(f"🗑️  Deleted {delete_result.deleted_count} existing test listings")

    # Generiraj in vstavi podatke v batch-ih
    batch_size = 1000
    total_inserted = 0

    for i in range(0, count, batch_size):
        batch = []
        current_batch_size = min(batch_size, count - i)

        for j in range(current_batch_size):
            listing = generate_listing(i + j + 1)
            batch.append(listing)

        # Vstavi batch
        try:
            collection.insert_many(batch)
            total_inserted += len(batch)
            progress = (total_inserted / count) * 100
            print(f"📝 Inserted {total_inserted:,}/{count:,} listings ({progress:.1f}%)")
        except Exception as e:
            print(f"❌ Error inserting batch: {e}")
            break

    print("-" * 60)
    print(f"✅ Successfully inserted {total_inserted:,} test listings")

    # Prikaži statistiko
    stats = {
        "Open": collection.count_documents({"status": "Open"}),
        "Forthcoming": collection.count_documents({"status": "Forthcoming"}),
        "Closed": collection.count_documents({"status": "Closed"})
    }

    print("\n📊 Database Statistics:")
    print(f"   Total listings: {collection.count_documents({}):,}")
    print(f"   Open: {stats['Open']:,}")
    print(f"   Forthcoming: {stats['Forthcoming']:,}")
    print(f"   Closed: {stats['Closed']:,}")

    client.close()
    print("\n✅ Done!")

if __name__ == "__main__":
    # Argumenti iz command line
    if len(sys.argv) < 2:
        print("Usage: python generate_test_data.py <count> [mongodb_uri] [db_name]")
        print("\nExamples:")
        print("  python generate_test_data.py 10000")
        print("  python generate_test_data.py 50000 mongodb://localhost:27017 smartsearch")
        sys.exit(1)

    count = int(sys.argv[1])
    mongodb_uri = sys.argv[2] if len(sys.argv) > 2 else "mongodb://localhost:27017"
    db_name = sys.argv[3] if len(sys.argv) > 3 else "smartsearch"

    seed_database(count, mongodb_uri, db_name)
