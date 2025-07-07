async function loadUserDetails() {
  const token = localStorage.getItem("token");

  if (!token) {
    document.getElementById("userDetails").textContent = "Not authenticated";
    alert("No authentication token found. Please log in.");
    window.location.href = "auth.html";
    return;
  }

  try {
    const res = await fetch("http://localhost:8081/api/user/profile", {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.status === 401 || res.status === 403) {
      throw new Error("Session expired or user not found");
    }

    if (!res.ok) {
      throw new Error("Failed to fetch user profile");
    }

    const data = await res.json();
    document.getElementById("userDetails").innerHTML =
      `Welcome <strong>${data.username}</strong>, System Role: <strong>${data.role}</strong>`;

  } catch (err) {
    console.error("Error fetching user info:", err.message);
    localStorage.removeItem("token");
    alert("Your session has expired or the user was deleted. Please log in again.");
    window.location.href = "auth.html";
  }
}

// Load user details when page loads
loadUserDetails();

document.getElementById("updateForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const token = localStorage.getItem("token");
  const newUsername = document.getElementById("newUsername").value;
  const messageBox = document.getElementById("message");

  if (!token) {
    messageBox.textContent = "Not authenticated.";
    return;
  }

  try {
    const res = await fetch("http://localhost:8081/api/user/profile/update-profile", {
      method: "PUT",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": `Bearer ${token}`
      },
      body: new URLSearchParams({ newUsername })
    });

    if (res.status === 401 || res.status === 403) {
      throw new Error("Unauthorized or expired session");
    }

    if (!res.ok) throw new Error("Failed to update username");

    const data = await res.json();
    messageBox.textContent = `Username updated to: ${data.username}`;
    messageBox.style.color = "lightgreen";

    // Optional: Update UI or force logout
    localStorage.removeItem("token");
    window.location.href = "auth.html";

  } catch (err) {
    console.error(err);
    messageBox.textContent = "Error: " + err.message;
    messageBox.style.color = "red";
  }
});
