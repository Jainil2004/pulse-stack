async function loadUserProfile() {
  const token = localStorage.getItem("token");
  
  if (!token) {
    console.error("No authentication token found");
    alert("No authentication token found. Please log in.");
    localStorage.removeItem("token");
    window.location.href = "auth.html";
    return;
  }

  try {
    const res = await fetch("http://localhost:8081/api/user/profile", {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`
      }
    });

    if (res.status === 401 || res.status === 403) {
      throw new Error("Session expired or user not found");
    }

    if (!res.ok) {
      throw new Error("Failed to fetch user profile");
    }

    const data = await res.json();
    // render user info
    document.getElementById("userDetails").innerHTML = `
      <p>Welcome, <strong>${data.username}</strong></p>
      <p>System Role: <strong>${data.role}</strong></p>
    `;
  } catch (err) {
    console.error("User session invalid:", err.message);
    alert("Your session has expired or the user was deleted. Please login again.");
    localStorage.removeItem("token");
    window.location.href = "auth.html";
  }
}


window.updateProfile = function () {
  window.location.href = "updateProfile.html";
};

window.logout = function () {
  localStorage.removeItem("token");
  alert("You have been logged out.");
  window.location.href = "auth.html";
};

window.deleteAccount = async function () {
  const confirmDelete = confirm("Are you sure you want to delete your account?");
  if (!confirmDelete) return;

  const currentToken = localStorage.getItem("token");
  
  try {
    const res = await fetch("http://localhost:8081/api/user/delete-account", {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${currentToken}`
      }
    });

    if (!res.ok) throw new Error("Deletion failed");

    alert("Account deleted.");
    localStorage.removeItem("token");
    window.location.href = "auth.html";
  } catch (err) {
    alert("Error deleting account.");
    console.error(err);
  }
};

loadUserProfile();
