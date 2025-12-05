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

// document.getElementById("registerBtn").onclick = async () => {
//   const token = localStorage.getItem("token");
//   const name = document.getElementById("systemName").value.trim();
//   const resultDiv = document.getElementById("registerResult");
  
//   if (!name) {
//     alert("System name is required");
//     return;
//   }

//   try {
//     const res = await fetch(`${BASE_URL}/api/systems/register`, {
//       method: "POST",
//       headers: {
//         "Content-Type": "application/x-www-form-urlencoded",
//         Authorization: `Bearer ${token}`,
//       },
//       body: new URLSearchParams({ systemName: name }),
//     });

//     if (!res.ok) throw new Error("Registration failed");
//     const data = await res.json();

//     resultDiv.innerHTML = `
//       <h4 style="color: #4CAF50; margin-bottom: 10px;">✅ System Registered Successfully!</h4>
//       <div style="background-color: rgba(34, 34, 34, 0.8); padding: 15px; border-radius: 8px; margin-bottom: 10px; overflow-wrap: break-word;">
//         <p style="margin: 5px 0;"><strong>System Name:</strong> ${data.systemName || name}</p>
//         <p style="margin: 5px 0;"><strong>System ID:</strong> <span style="color: #64B5F6;">${data.systemId}</span></p>
//         <p style="margin: 5px 0; overflow-wrap: break-word; word-wrap: break-word; word-break: break-all; max-width: 100%;"><strong>System Auth Token:</strong> <span style="color: #81C784; display: inline-block; max-width: 100%; overflow-wrap: break-word; word-wrap: break-word; word-break: break-all;">${data.authToken}</span></p>
//       </div>
//       <p style="color: #FFB74D; font-weight: bold; margin: 10px 0;">
//         ⚠️ Please copy the AuthToken and SystemID for use in your client script.
//       </p>
//       <p style="color: #F44336; font-weight: bold; margin: 10px 0;">
//         ⚠️ Ensure you copy the AuthToken as it will not be accessible later
//       </p>
//     `;
//     resultDiv.style.display = "block";
    
//     // Clear the input and refresh the systems list
//     document.getElementById("systemName").value = "";
//     fetchSystems();
//   } catch (err) {
//     resultDiv.innerHTML = `
//       <h4 style="color: #F44336; margin-bottom: 10px;">❌ Registration Failed</h4>
//       <p style="color: #FFCDD2;">Failed to register system. Please try again.</p>
//     `;
//     resultDiv.style.display = "block";
//     console.error(err);
//   }
// };

// Replace your existing "registerBtn.onclick" function with this one
document.getElementById("registerBtn").onclick = async () => {
  const token = localStorage.getItem("token");
  const name = document.getElementById("systemName").value.trim();
  const resultDiv = document.getElementById("registerResult");

  if (!name) {
    alert("System name is required");
    return;
  }

  // Clear previous results
  resultDiv.style.display = "none";
  resultDiv.innerHTML = "";

  try {
    const res = await fetch(`${BASE_URL}/api/systems/register`, { // ⚠️ Double-check this URL!
      method: "POST",
      headers: {
        // IMPORTANT: The browser sets the Content-Type for URLSearchParams automatically.
        // It's often better to let it handle it. But if you must set it:
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${token}`,
      },
      body: new URLSearchParams({ systemName: name }),
    });

    if (!res.ok) {
      // If the name already exists, the server should return a 409 Conflict
      if (res.status === 409) {
          throw new Error(`Registration failed: A system with the name "${name}" already exists.`);
      }
      throw new Error(`Registration failed with status: ${res.status}`);
    }

    // --- THIS IS THE NEW FILE DOWNLOAD LOGIC ---

    // 1. Get the filename from the 'Content-Disposition' header
    const disposition = res.headers.get('Content-Disposition');
    let filename = 'client-config.json'; // Provide a default
    if (disposition && disposition.includes('attachment')) {
      const filenameMatch = /filename="([^"]+)"/.exec(disposition);
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1];
      }
    }

    // 2. Get the response body as a Blob (raw file data)
    const blob = await res.blob();

    // 3. Create a temporary hidden link element to trigger the download
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = filename; // Use the filename we extracted

    // 4. Trigger the download and clean up
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);

    // --- END OF NEW LOGIC ---

    // Provide feedback to the user
    resultDiv.innerHTML = `<h4 style="color: #4CAF50;">✅ Success! Your configuration file is downloading.</h4>`;
    resultDiv.style.display = "block";
    
    // Clear the input and refresh the systems list
    document.getElementById("systemName").value = "";
    fetchSystems(); // This is great, it keeps your list up-to-date

  } catch (err) {
    resultDiv.innerHTML = `
      <h4 style="color: #F44336; margin-bottom: 10px;">❌ Registration Failed</h4>
      <p style="color: #FFCDD2;">${err.message}</p>
    `;
    resultDiv.style.display = "block";
    console.error(err);
  }
};

async function fetchSystems() {
  const token = localStorage.getItem("token");
  const tableBody = document.querySelector("#systemTable tbody");

  if (!token) {
    alert("No authentication token found. Please log in.");
    window.location.href = "auth.html";
    return;
  }

  try {
    const res = await fetch(`${BASE_URL}/api/systems`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (res.status === 401 || res.status === 403) {
      throw new Error("Session expired or access denied");
    }

    if (!res.ok) throw new Error("Failed to fetch systems");

    const systems = await res.json();
    tableBody.innerHTML = "";

    if (systems.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="3" style="text-align: center; color: #B0B0B0; font-style: italic;">
            No systems registered yet. Register your first system above!
          </td>
        </tr>
      `;
      return;
    }

    systems.forEach(system => {
      const row = document.createElement("tr");

      row.innerHTML = `
        <td style="font-weight: bold;">${system.name}</td>
        <td>${system.registeredAt || "-"}</td>
        <td>
          <button onclick="openDashboard('${system.systemId}')" style="background-color: rgba(34, 139, 34, 0.8);">RT Dashboard</button>
          <button onclick="updateSystem('${system.name}')" style="background-color: rgba(255, 165, 0, 0.8);">Update Machine Name</button>
          <button onclick="deleteSystem('${system.name}')" style="background-color: rgba(220, 20, 60, 0.8);">Remove Machine</button>
        </td>
      `;

      tableBody.appendChild(row);
    });

  } catch (err) {
    console.error("System fetch error:", err);
    if (err.message.includes("Session expired") || err.message.includes("access denied")) {
      alert("Session expired or access denied. Please log in again.");
      localStorage.removeItem("token");
      window.location.href = "auth.html";
    } else {
      alert("Failed to fetch systems. Please try again.");
    }
  }
}

async function deleteSystem(name) {
  const token = localStorage.getItem("token");
  
  if (!confirm(`Are you sure you want to delete system "${name}"?`)) return;

  try {
    const res = await fetch(`${BASE_URL}/api/systems/${name}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (res.status === 401 || res.status === 403) {
      throw new Error("Session expired or access denied");
    }

    if (!res.ok) throw new Error("Delete failed");

    alert(`System "${name}" deleted successfully`);
    fetchSystems(); // Refresh table
  } catch (err) {
    if (err.message.includes("Session expired")) {
      alert("Session expired. Please log in again.");
      localStorage.removeItem("token");
      window.location.href = "auth.html";
    } else {
      alert("Failed to delete system");
    }
    console.error(err);
  }
}

function updateSystem(name) {
  // Redirect to update-system.html with query param
  window.location.href = `update_system.html?name=${encodeURIComponent(name)}`;
}

function openDashboard(systemId) {
  // Redirect to dashboard.html with systemId as query parameter
  window.location.href = `dashboard.html?systemId=${systemId}`;
}

// Initialize the page
loadUserDetails();
fetchSystems();
