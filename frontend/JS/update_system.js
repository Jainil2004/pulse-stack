const BASE_URL = "http://localhost:8081";

async function loadUserDetails() {
  const token = localStorage.getItem("token");

  if (!token) {
    document.getElementById("userDetails").textContent = "Not authenticated";
    alert("No authentication token found. Please log in.");
    window.location.href = "auth.html";
    return;
  }

  try {
    const res = await fetch(`${BASE_URL}/api/user/profile`, {
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

function getQueryParam(param) {
  const urlParams = new URLSearchParams(window.location.search);
  console.log(urlParams.get(param))
  return urlParams.get(param);
}

const systemName = getQueryParam("name");

if (!systemName) {
  alert("No system specified for update.");
  window.location.href = "system.html";
}

const token = localStorage.getItem("token");
if (!token) {
  alert("No auth token found. Please log in.");
  window.location.href = "auth.html";
}

const currentNameInput = document.getElementById("currentName");
const newNameInput = document.getElementById("newName");
const updateForm = document.getElementById("updateForm");
const resultDiv = document.getElementById("updateResult");

async function loadSystemDetails() {
  try {
    const res = await fetch(`${BASE_URL}/api/systems/get/${systemName}`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) throw new Error("Failed to fetch system details");
    const data = await res.json();

    currentNameInput.value = data.name || systemName;
  } catch (err) {
    console.error("Error:", err);
    alert("Unable to load system details.");
    window.location.href = "system.html";
  }
}

updateForm.addEventListener("submit", async (e) => {
  e.preventDefault();

  const newName = newNameInput.value.trim();
  if (!newName) {
    alert("New system name is required.");
    return;
  }

  try {
    const res = await fetch(`${BASE_URL}/api/systems/update/${systemName}`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ newName: newName }),
    });

    if (!res.ok) throw new Error("Update failed");

    const data = await res.json();

    resultDiv.innerHTML = `
      <h4 style="color: #4CAF50;">✅ System updated successfully!</h4>
      <p>New name: <strong>${data.name}</strong></p>
      <p><a href="system.html" style="color: #64B5F6;">← Back to system panel</a></p>
    `;
    resultDiv.style.display = "block";
    currentNameInput.value = data.name;
    newNameInput.value = "";
  } catch (err) {
    console.error(err);
    resultDiv.innerHTML = `
      <h4 style="color: #F44336;">❌ Update failed</h4>
      <p>Please try again or contact support.</p>
    `;
    resultDiv.style.display = "block";
  }
});

loadUserDetails();
loadSystemDetails();
