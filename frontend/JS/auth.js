const BASE_URL = "http://localhost:8081"; // Your Spring Boot backend

// Clear any existing token when the auth page loads
localStorage.clear();
sessionStorage.clear();

document.getElementById("show-login").onclick = () => {
  document.getElementById("login-form").classList.remove("hidden");
  document.getElementById("register-form").classList.add("hidden");
  // Clear any error messages
  document.getElementById("login-error").textContent = "";
  document.getElementById("register-error").textContent = "";
};

document.getElementById("show-register").onclick = () => {
  document.getElementById("register-form").classList.remove("hidden");
  document.getElementById("login-form").classList.add("hidden");
  // Clear any error messages
  document.getElementById("login-error").textContent = "";
  document.getElementById("register-error").textContent = "";
};

// ------------------- LOGIN -------------------
document.getElementById("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = document.getElementById("login-name").value;
  const password = document.getElementById("login-password").value;

  try {
    const res = await fetch(`${BASE_URL}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({ username, password }).toString(),
    });

    if (!res.ok) throw new Error("Invalid credentials");
    const token = await res.text();
    
    // Ensure token is properly stored
    localStorage.setItem("token", token);
    console.log("Login successful, token stored");
    window.location.href = "home.html";
  } catch (err) {
    document.getElementById("login-error").textContent = err.message;
  }
});

// ------------------- REGISTER -------------------
document.getElementById("register-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = document.getElementById("register-name").value;
  const password = document.getElementById("register-password").value;

  try {
    const res = await fetch(`${BASE_URL}/api/auth/register`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({ username, password }).toString(),
    });

    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(errorText || "Registration failed");
    }
    
    // Clear any existing token first
    localStorage.removeItem("token");
    
    // Registration successful - now automatically log in the user
    try {
      const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({ username, password }).toString(),
      });

      if (loginRes.ok) {
        const token = await loginRes.text();
        localStorage.setItem("token", token);
        window.location.href = "home.html";
      } else {
        // Registration succeeded but auto-login failed, switch to login form
        alert("Registration successful! Please log in.");
        document.getElementById("register-form").classList.add("hidden");
        document.getElementById("login-form").classList.remove("hidden");
        // Pre-fill the username
        document.getElementById("login-name").value = username;
        document.getElementById("register-name").value = "";
        document.getElementById("register-password").value = "";
      }
    } catch (loginErr) {
      console.error("Auto-login failed:", loginErr);
      alert("Registration successful! Please log in.");
      document.getElementById("register-form").classList.add("hidden");
      document.getElementById("login-form").classList.remove("hidden");
      document.getElementById("login-name").value = username;
      document.getElementById("register-name").value = "";
      document.getElementById("register-password").value = "";
    }
  } catch (err) {
    document.getElementById("register-error").textContent = err.message;
  }
});
