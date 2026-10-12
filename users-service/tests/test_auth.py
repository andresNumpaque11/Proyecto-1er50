def register(client, email="person@example.com", password="safe-password-123"):
    return client.post(
        "/api/auth/register",
        json={"full_name": "Test Person", "email": email, "password": password},
    )


def test_register_login_and_get_profile(client):
    created = register(client)
    assert created.status_code == 201
    assert created.json()["role"] == "user"
    assert "hashed_password" not in created.json()

    logged_in = client.post(
        "/api/auth/login",
        json={"email": "PERSON@example.com", "password": "safe-password-123"},
    )
    assert logged_in.status_code == 200
    token = logged_in.json()["access_token"]
    assert logged_in.json()["token_type"] == "bearer"

    profile = client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert profile.status_code == 200
    assert profile.json()["email"] == "person@example.com"


def test_duplicate_email_and_invalid_login_are_rejected(client):
    assert register(client).status_code == 201
    assert register(client).status_code == 409
    response = client.post(
        "/api/auth/login",
        json={"email": "person@example.com", "password": "wrong-password"},
    )
    assert response.status_code == 401


def test_users_management_requires_admin(client):
    register(client)
    token = client.post(
        "/api/auth/login",
        json={"email": "person@example.com", "password": "safe-password-123"},
    ).json()["access_token"]
    response = client.get("/api/users", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 403


def test_admin_can_manage_users_but_cannot_disable_last_admin(client):
    registered = register(client)
    user_id = registered.json()["id"]
    admin_token = client.post(
        "/api/auth/login",
        json={"email": "admin@example.com", "password": "admin-password-123"},
    ).json()["access_token"]
    headers = {"Authorization": f"Bearer {admin_token}"}

    listing = client.get("/api/users", headers=headers)
    assert listing.status_code == 200
    assert len(listing.json()) == 2
    assert "hashed_password" not in listing.json()[0]

    admin_id = next(item["id"] for item in listing.json() if item["email"] == "admin@example.com")
    deactivated = client.patch(
        f"/api/users/{admin_id}/status",
        json={"is_active": False},
        headers=headers,
    )
    assert deactivated.status_code == 409

    promoted = client.patch(
        f"/api/users/{user_id}/role",
        json={"role": "admin"},
        headers=headers,
    )
    assert promoted.status_code == 200
    assert promoted.json()["role"] == "admin"

    user_token = client.post(
        "/api/auth/login",
        json={"email": "person@example.com", "password": "safe-password-123"},
    ).json()["access_token"]
    disabled = client.patch(
        f"/api/users/{user_id}/status",
        json={"is_active": False},
        headers=headers,
    )
    assert disabled.status_code == 200
    profile_after_disable = client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {user_token}"},
    )
    assert profile_after_disable.status_code == 401


def test_protected_route_rejects_missing_and_invalid_tokens(client):
    assert client.get("/api/auth/me").status_code == 401
    assert client.get(
        "/api/auth/me", headers={"Authorization": "Bearer not-a-token"}
    ).status_code == 401
