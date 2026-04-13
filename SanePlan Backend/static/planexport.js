// --- EXPORT + POST LOGIC ---
document.getElementById('exportForm').addEventListener('submit', async function(e) {
  e.preventDefault();

  const board = document.querySelector('.board');
  const planName = board.id || "My Plan";

  const semesters = [];

  document.querySelectorAll('.column').forEach(col => {
    const title = col.querySelector('h3').innerText.trim();
    const type = col.dataset.type;

	const courses = Array.from(col.querySelectorAll('.course')).map(c => {
	  return c.id;
	});

    semesters.push({
      name: title,
      maxCredits: 18,
      type: type,
      courses: courses
    });
  });

  const payload = {
    name: planName,
    validityIssues: [],
    semesters: semesters
  };

  try {
    const response = await fetch('/submitPlan', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (response.ok) {
      alert('Plan successfully submitted!');
	  window.location.reload();
    } else {
      alert('Submission failed: ' + response.status);
    }
  } catch (err) {
    console.error(err);
    alert('Error submitting plan.');
  }

  console.log(JSON.stringify(payload, null, 2));
});