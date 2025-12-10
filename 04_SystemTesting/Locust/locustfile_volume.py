#!/usr/bin/env python3
"""
Locust Volume Testing Suite for SmartSearch API
Testira performance API-jev z različnimi volumni podatkov v MongoDB
"""

from locust import HttpUser, task, between, events
from locust.runners import MasterRunner, WorkerRunner
import random
import json
import time
from datetime import datetime
import os
from dotenv import load_dotenv

# Naloži environment variables
load_dotenv()

# Konfiguracija
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


class SmartSearchVolumeTestUser(HttpUser):
    """
    Simulira uporabnika, ki izvaja različne API klice
    za testiranje performance z velikimi volumni podatkov
    """

    # Čakalni čas med zahtevki (1-3 sekunde)
    wait_time = between(1, 3)

    def on_start(self):
        """Izvede se ob začetku vsakega uporabnika"""
        print(f"🟢 User {id(self)} started")
        self.search_keywords = [
            "innovation", "technology", "research", "artificial intelligence",
            "healthcare", "energy", "blockchain", "robotics", "sustainability",
            "digital", "quantum", "biotechnology"
        ]

    @task(10)  # Prioriteta 10 - najpogostejši klic
    def get_all_listings(self):
        """
        GET /api/listings/show/all
        Najpogostejši API klic - pridobi vse listings
        """
        with self.client.get(
            "/api/listings/show/all",
            catch_response=True,
            name="GET /api/listings/show/all"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    listing_count = len(data)
                    response_time = response.elapsed.total_seconds() * 1000

                    # Uspešen response
                    response.success()

                    # Log metrike
                    print(f"✅ All listings: {listing_count} items, {response_time:.0f}ms")

                    # Preveri performance threshold
                    if response_time > 2000:  # Več kot 2 sekundi
                        print(f"⚠️  SLOW RESPONSE: {response_time:.0f}ms")

                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Failed with status {response.status_code}")

    @task(8)  # Prioriteta 8
    def get_open_listings(self):
        """
        GET /api/listings/show/open
        Pridobi samo odprte razpise
        """
        with self.client.get(
            "/api/listings/show/open",
            catch_response=True,
            name="GET /api/listings/show/open"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    response_time = response.elapsed.total_seconds() * 1000
                    response.success()
                    print(f"✅ Open listings: {len(data)} items, {response_time:.0f}ms")
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Failed with status {response.status_code}")

    @task(5)  # Prioriteta 5
    def get_forthcoming_listings(self):
        """
        GET /api/listings/show/forthcoming
        Pridobi prihajajoče razpise
        """
        with self.client.get(
            "/api/listings/show/forthcoming",
            catch_response=True,
            name="GET /api/listings/show/forthcoming"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                response_time = response.elapsed.total_seconds() * 1000
                response.success()
                print(f"✅ Forthcoming: {len(data)} items, {response_time:.0f}ms")
            else:
                response.failure(f"Failed with status {response.status_code}")

    @task(5)  # Prioriteta 5
    def get_closed_listings(self):
        """
        GET /api/listings/show/closed
        Pridobi zaprte razpise
        """
        with self.client.get(
            "/api/listings/show/closed",
            catch_response=True,
            name="GET /api/listings/show/closed"
        ) as response:
            if response.status_code == 200:
                data = response.json()
                response_time = response.elapsed.total_seconds() * 1000
                response.success()
                print(f"✅ Closed: {len(data)} items, {response_time:.0f}ms")
            else:
                response.failure(f"Failed with status {response.status_code}")

    @task(3)  # Prioriteta 3
    def get_management_status(self):
        """
        GET /api/management/status
        Pridobi statistiko sistema
        """
        with self.client.get(
            "/api/management/status",
            catch_response=True,
            name="GET /api/management/status"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    response_time = response.elapsed.total_seconds() * 1000
                    response.success()

                    # Prikaži statistiko
                    total = data.get("totalListings", 0)
                    print(f"📊 Status: {total} total listings, {response_time:.0f}ms")

                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Failed with status {response.status_code}")

    @task(2)  # Prioriteta 2
    def search_with_ai_keywords(self):
        """
        POST /api/openai/keywords
        Testiranje AI keyword extraction (če je API ključ nastavljen)
        """
        keyword = random.choice(self.search_keywords)
        user_input = f"I am looking for funding opportunities in {keyword}"

        with self.client.post(
            "/api/openai/keywords",
            json={"userInput": user_input},
            catch_response=True,
            name="POST /api/openai/keywords"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    response_time = response.elapsed.total_seconds() * 1000

                    # API lahko vrne list ali dict
                    if isinstance(data, dict):
                        keywords = data.get("keywords", [])
                    elif isinstance(data, list):
                        keywords = data
                    else:
                        keywords = []

                    response.success()
                    print(f"🔍 AI Keywords: {keywords}, {response_time:.0f}ms")
                except (json.JSONDecodeError, AttributeError) as e:
                    response.failure(f"Invalid response: {e}")
            elif response.status_code == 500:
                # API ključ ni nastavljen ali OpenAI napaka
                print("⚠️  OpenAI API not available (skipping)")
                response.success()  # Ne štej kot napako
            else:
                response.failure(f"Failed with status {response.status_code}")


# Event handlers za statistiko

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Izvede se ob začetku testa"""
    print("\n" + "="*70)
    print("🦗 LOCUST VOLUME TESTING - SmartSearch API")
    print("="*70)
    print(f"🎯 Target: {API_BASE_URL}")
    print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70 + "\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Izvede se ob koncu testa"""
    print("\n" + "="*70)
    print("🏁 TEST COMPLETED")
    print("="*70)
    print(f"⏰ Finished at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # Pridobi statistiko
    stats = environment.stats
    print(f"\n📊 SUMMARY:")
    print(f"   Total requests: {stats.total.num_requests}")
    print(f"   Failed requests: {stats.total.num_failures}")
    print(f"   Median response time: {stats.total.median_response_time}ms")
    print(f"   95th percentile: {stats.total.get_response_time_percentile(0.95)}ms")
    print(f"   Requests/sec: {stats.total.total_rps:.2f}")
    print("="*70 + "\n")


# Event handler za request monitoring - odstranjen zaradi compatibility issues
# @events.request.add_listener
# def on_request(**kwargs):
#     """
#     Izvede se ob vsakem API zahtevku
#     Uporablja se za real-time monitoring
#     """
#     exception = kwargs.get('exception')
#     name = kwargs.get('name')
#     if exception:
#         print(f"❌ Request failed: {name} - {exception}")
