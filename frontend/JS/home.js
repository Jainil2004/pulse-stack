document.getElementById("user-panel").onclick = () => {
  window.location.href = "user.html";
};

document.getElementById("admin-panel").onclick = () => {
  window.location.href = "admin.html";
};

document.getElementById("system-panel").onclick = () => {
  window.location.href = "system.html";
};

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

loadUserDetails()
