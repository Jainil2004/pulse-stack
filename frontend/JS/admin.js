async function loadAdminDetails() {
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
      throw new Error("Session expired or access denied");
    }

    if (!res.ok) {
      throw new Error("Failed to fetch admin profile");
    }

    const data = await res.json();
    
    if (data.role !== "ADMIN") {
      alert("Access denied. Admin privileges required.");
      window.location.href = "home.html";
      return;
    }

    // Store current user info globally for self-deletion prevention
    window.currentUser = {
      id: data.id,
      username: data.username,
      role: data.role
    };

    document.getElementById("userDetails").innerHTML =
      `Welcome <strong>${data.username}</strong>, System Role: <strong>${data.role}</strong>`;

  } catch (err) {
    console.error("Error fetching admin info:", err.message);
    localStorage.removeItem("token");
    alert("Your session has expired or access was denied. Please log in again.");
    window.location.href = "auth.html";
  }
}

async function fetchUsers() {
  const token = localStorage.getItem("token");
  const tableBody = document.querySelector("#userTable tbody");

  try {
    const res = await fetch("http://localhost:8081/api/admin/users", {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Failed to fetch users");

    const users = await res.json();
    tableBody.innerHTML = "";

    users.forEach(user => {
      const row = document.createElement("tr");
      
      // Check if this is the current logged-in user
      const isCurrentUser = window.currentUser && (user.id === window.currentUser.id || user.username === window.currentUser.username);
      
      // Create delete button - disabled for current user
      const deleteButton = isCurrentUser 
        ? `<button onclick="preventSelfDeletion()" style="background-color: rgba(100, 100, 100, 0.5); cursor: not-allowed;" disabled>Cannot Remove Self</button>`
        : `<button onclick="deleteUser(${user.id})" style="background-color: rgba(220, 20, 60, 0.8);">Remove User</button>`;

      row.innerHTML = `
        <td>${user.id}</td>
        <td>${user.username}${isCurrentUser ? ' <strong>(You)</strong>' : ''}</td>
        <td>
          <select id="role-${user.id}">
            <option value="USER" ${user.role === "USER" ? "selected" : ""}>USER</option>
            <option value="ADMIN" ${user.role === "ADMIN" ? "selected" : ""}>ADMIN</option>
          </select>
        </td>
        <td>
          <button onclick="changeRole(${user.id})">Change User Role</button>
          ${deleteButton}
        </td>
      `;

      tableBody.appendChild(row);
    });

  } catch (err) {
    console.error("Admin fetch error:", err);
  }
}

async function updateUser(id) {
  const token = localStorage.getItem("token");
  const username = document.getElementById(`username-${id}`).value;

  try {
    const res = await fetch(`http://localhost:8081/api/admin/users/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${token}`,
      },
      body: new URLSearchParams({ username }),
    });

    if (!res.ok) throw new Error("Update failed");
    alert("Username updated!");
  } catch (err) {
    alert("Failed to update user");
    console.error(err);
  }
}

async function changeRole(id) {
  const token = localStorage.getItem("token");
  const role = document.getElementById(`role-${id}`).value;

  try {
    const res = await fetch(`http://localhost:8081/api/admin/users/${id}/role`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${token}`,
      },
      body: new URLSearchParams({ newRole: role }), 
    });

    if (!res.ok) {
      const errorMsg = await res.text();
      throw new Error(errorMsg || "Failed to change role");
    }

    alert("Role changed!");
  } catch (err) {
    alert("Failed to change user role: " + err.message);
    console.error(err);
  }
}

async function deleteUser(id) {
  const token = localStorage.getItem("token");

  if (!confirm("Are you sure you want to delete this user?")) return;

  try {
    const res = await fetch(`http://localhost:8081/api/admin/users/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Delete failed");

    alert("User deleted");
    fetchUsers(); // refresh list
  } catch (err) {
    alert("Failed to delete user");
    console.error(err);
  }
}

function preventSelfDeletion() {
  alert("You cannot delete your own admin account while logged in.");
}

loadAdminDetails();
fetchUsers();
