const API_URL = "/incident/analyze";

const app = document.querySelector("#app");
const form = document.querySelector("#incidentForm");
const description = document.querySelector("#description");
const sampleButton = document.querySelector("#sampleButton");
const submitButton = document.querySelector("#submitButton");
const charHint = document.querySelector("#charHint");
const resultPanel = document.querySelector("#resultPanel");
const decisionTitle = document.querySelector("#decisionTitle");
const decisionBadge = document.querySelector("#decisionBadge");
const summaryText = document.querySelector("#summaryText");
const decisionBody = document.querySelector("#decisionBody");

const fixedIncident = {
  scenario: "OUT_OF_FUEL",
  latitude: "40.23839724009791",
  longitude: "-74.0125940932132"
};

const serviceProfiles = {
  FUEL_DELIVERY: {
    title: "Fuel is on the way",
    badge: "Fuel delivery",
    tone: "success",
    provider: "Allstate Roadside Fuel Assist",
    eta: "18 minutes",
    detail: "A nearby fuel delivery partner has accepted your request."
  },
  TOW_TRUCK: {
    title: "Tow help is on the way",
    badge: "Tow truck",
    tone: "warning",
    provider: "Allstate Roadside Tow Assist",
    eta: "24 minutes",
    detail: "A tow partner has accepted your request and is heading to your location."
  }
};

const loadingSteps = [
  "Reviewing your description",
  "Checking nearby fuel stations",
  "Selecting the safest option"
];

let loadingTimer = null;

sampleButton.addEventListener("click", () => {
  description.value = "NO fuel can you please arrange";
  description.dispatchEvent(new Event("input", { bubbles: true }));
  description.focus();
});

description.addEventListener("input", () => {
  const length = description.value.trim().length;
  charHint.textContent = length ? `${length} characters` : "";
  app.dataset.state = length ? "typing" : app.dataset.state === "result" ? "result" : "idle";
});

decisionBody.addEventListener("click", (event) => {
  const fuelDeliveryButton = event.target.closest("#fuelDeliveryOption");
  const uberChoiceButton = event.target.closest("[data-uber-choice]");

  if (fuelDeliveryButton) {
    renderDispatch("FUEL_DELIVERY");
    return;
  }

  if (uberChoiceButton) {
    renderUberMessage(uberChoiceButton.dataset.uberChoice === "yes");
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const text = description.value.trim();

  if (!text) {
    description.focus();
    description.classList.add("input-error");
    return;
  }

  description.classList.remove("input-error");
  setLoading(true);

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        ...fixedIncident,
        description: text
      })
    });

    const payload = await response.json();

    if (!response.ok) {
      throw new Error(payload.error || "We could not complete your request.");
    }

    renderAnalysis(payload);
  } catch (error) {
    renderError(error);
  } finally {
    setLoading(false);
  }
});

function setAppState(state) {
  app.dataset.state = state;
}

function showResultPanel() {
  resultPanel.hidden = false;
  requestAnimationFrame(() => {
    resultPanel.classList.add("is-visible");
  });
}

function setLoading(isLoading) {
  form.classList.toggle("loading", isLoading);
  submitButton.disabled = isLoading;
  submitButton.querySelector(".button-label").textContent = isLoading
    ? "Finding help..."
    : "Get help now";

  if (isLoading) {
    setAppState("loading");
    showResultPanel();
    decisionTitle.textContent = "Finding your best help option";
    decisionBadge.textContent = "Checking";
    decisionBadge.className = "decision-badge";
    summaryText.textContent = "Please stay in a safe place while we review your request.";
    decisionBody.className = "decision-body empty-state loading-state";
    decisionBody.innerHTML = `
      <div class="loading-steps" id="loadingSteps"></div>
      <p>We are checking nearby help options.</p>
    `;
    startLoadingSteps();
    return;
  }

  stopLoadingSteps();
}

function startLoadingSteps() {
  stopLoadingSteps();
  const container = document.querySelector("#loadingSteps");

  if (!container) {
    return;
  }

  let stepIndex = 0;
  container.innerHTML = loadingSteps
    .map(
      (step, index) => `
        <div class="loading-step" data-step="${index}">
          <span class="loading-dot" aria-hidden="true"></span>
          <span>${escapeHtml(step)}</span>
        </div>
      `
    )
    .join("");

  const steps = [...container.querySelectorAll(".loading-step")];
  steps[0]?.classList.add("is-active");

  loadingTimer = window.setInterval(() => {
    stepIndex = Math.min(stepIndex + 1, steps.length - 1);
    steps.forEach((step, index) => {
      step.classList.toggle("is-active", index === stepIndex);
      step.classList.toggle("is-complete", index < stepIndex);
    });

    if (stepIndex >= steps.length - 1) {
      stopLoadingSteps();
    }
  }, 900);
}

function stopLoadingSteps() {
  if (loadingTimer) {
    window.clearInterval(loadingTimer);
    loadingTimer = null;
  }
}

function renderAnalysis(payload) {
  setAppState("result");
  showResultPanel();

  const recommendations = Array.isArray(payload.recommendations)
    ? payload.recommendations
    : [];
  const topRecommendation =
    recommendations.find((item) => Number(item.rank) === 1) || recommendations[0];
  const action = normalizeAction(topRecommendation?.action);

  if (payload.summary) {
    summaryText.textContent = payload.summary;
  }

  if (action === "WALK_TO_FUEL_STATION") {
    renderWalkRecommendation(payload);
    return;
  }

  if (action === "FUEL_DELIVERY" || action === "TOW_TRUCK") {
    renderDispatch(action, payload);
    return;
  }

  renderFallback(payload);
}

function renderWalkRecommendation(payload) {
  const distanceLabel = formatDistanceKm(payload?.nearestFuelPump);

  decisionTitle.textContent = "A fuel station is close by";
  decisionBadge.textContent = "Safe option";
  decisionBadge.className = "decision-badge success";
  summaryText.textContent =
    payload.summary ||
    "You can walk to a nearby fuel station if you feel safe doing so.";

  decisionBody.className = "decision-body reveal-content";
  decisionBody.innerHTML = `
    ${
      distanceLabel
        ? `
          <div class="distance-card">
            <span class="distance-value">${escapeHtml(distanceLabel.value)}</span>
            <span class="distance-label">${escapeHtml(distanceLabel.label)}</span>
          </div>
        `
        : ""
    }
    <div class="action-callout">
      <div class="customer-action">
        <strong>Walk only if the area feels safe.</strong>
        <span>Keep your phone with you and stay aware of traffic. If you prefer not to walk, we can send fuel to you.</span>
      </div>
      <div class="button-row">
        <button class="secondary-button" type="button" id="fuelDeliveryOption">
          Get fuel delivered
        </button>
      </div>
    </div>
  `;
}

function renderDispatch(action, payload) {
  const profile = serviceProfiles[action];
  const showUber = action === "TOW_TRUCK";

  decisionTitle.textContent = profile.title;
  decisionBadge.textContent = profile.badge;
  decisionBadge.className = `decision-badge ${profile.tone}`;
  summaryText.textContent =
    payload?.summary || "We contacted nearby help for you.";
  decisionBody.className = "decision-body reveal-content";
  decisionBody.innerHTML = `
    <div class="action-callout">
      <div class="customer-action">
        <strong>${escapeHtml(profile.title)}</strong>
        <span>${escapeHtml(profile.detail)}</span>
      </div>

      <div class="service-card">
        <strong>${escapeHtml(profile.provider)}</strong>
        <span>Your location has been shared with the assigned roadside partner.</span>
        <span class="eta-pill">ETA ${escapeHtml(profile.eta)}</span>
      </div>

      ${showUber ? renderUberPrompt() : ""}
    </div>
  `;
}

function renderUberPrompt() {
  return `
    <div class="ride-box">
      <h3>Need an Uber?</h3>
      <p>If your vehicle is being towed, we can note that you need a ride.</p>
      <div class="button-row" aria-label="Uber request">
        <button class="secondary-button" type="button" data-uber-choice="yes">
          Yes, I need a ride
        </button>
        <button class="ghost-button" type="button" data-uber-choice="no">
          No, thanks
        </button>
      </div>
      <p id="uberMessage" class="micro-message"></p>
    </div>
  `;
}

function renderUberMessage(wantsUber) {
  const message = document.querySelector("#uberMessage");

  if (!message) {
    return;
  }

  message.textContent = wantsUber
    ? "Ride request noted. A support agent can help coordinate pickup."
    : "No ride requested. Tow assistance remains active.";
}

function renderFallback(payload) {
  decisionTitle.textContent = "Help is being reviewed";
  decisionBadge.textContent = "Review";
  decisionBadge.className = "decision-badge";
  summaryText.textContent =
    payload?.summary ||
    "We need a support agent to review this request before confirming the next step.";
  decisionBody.className = "decision-body reveal-content";
  decisionBody.innerHTML = `
    <div class="action-callout">
      <div class="customer-action">
        <strong>Please stay safe while we review this.</strong>
        <span>Move away from traffic if possible and keep your phone nearby.</span>
      </div>
    </div>
  `;
}

function renderError(error) {
  setAppState("error");
  showResultPanel();
  decisionTitle.textContent = "We could not find help yet";
  decisionBadge.textContent = "Try again";
  decisionBadge.className = "decision-badge danger";
  summaryText.textContent =
    "Please check the service and try again. If this is an emergency, call local emergency services.";
  decisionBody.className = "decision-body empty-state reveal-content";
  decisionBody.innerHTML = `
    <div class="help-icon" aria-hidden="true">!</div>
    <p>${escapeHtml(error.message || "The request could not be completed.")}</p>
  `;
}

function formatDistanceKm(km) {
  const value = Number(km);

  if (!Number.isFinite(value) || value <= 0) {
    return null;
  }

  if (value < 1) {
    return {
      value: `${Math.round(value * 1000)} m`,
      label: "to the nearest fuel station"
    };
  }

  return {
    value: `${value.toFixed(1)} km`,
    label: "to the nearest fuel station"
  };
}

function normalizeAction(action) {
  return String(action || "")
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_");
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
