const courses = document.querySelectorAll('.course');
const columns = document.querySelectorAll('.column');
let dragged = null;

courses.forEach(course => {
  course.addEventListener('dragstart', () => {
    dragged = course;
  });
});

columns.forEach(column => {
  column.addEventListener('dragover', e => {
    e.preventDefault();
  });

  column.addEventListener('drop', e => {
    e.preventDefault();
    if (dragged) {
      column.appendChild(dragged);
      drawArrows();
    }
  });
});

function getEdgePoint(rect, targetX, targetY) {
  const cx = rect.left + rect.width / 2 + window.scrollX;
  const cy = rect.top + rect.height / 2 + window.scrollY;

  const dx = targetX - cx;
  const dy = targetY - cy;

  const w = rect.width / 2;
  const h = rect.height / 2;

  const scaleX = dx !== 0 ? Math.abs(w / dx) : Infinity;
  const scaleY = dy !== 0 ? Math.abs(h / dy) : Infinity;

  const t = Math.min(scaleX, scaleY);

  return {
    x: cx + dx * t,
    y: cy + dy * t
  };
}

function drawArrows() {
  const svg = document.getElementById('arrows');
  svg.innerHTML = '';
  svg.setAttribute('width', document.body.scrollWidth);
  svg.setAttribute('height', document.body.scrollHeight);

  addArrowMarker();

  document.querySelectorAll('.course').forEach(course => {
    const prereqs = course.dataset.prereq
      .split(',')
      .map(p => p.trim())
      .filter(p => p.length > 0);

    prereqs.forEach(pr => {
      const prereqEl = document.getElementById(pr);
      if (!prereqEl) return;

      const rect1 = prereqEl.getBoundingClientRect();
      const rect2 = course.getBoundingClientRect();

      const center1 = {
        x: rect1.left + rect1.width / 2 + window.scrollX,
        y: rect1.top + rect1.height / 2 + window.scrollY
      };

      const center2 = {
        x: rect2.left + rect2.width / 2 + window.scrollX,
        y: rect2.top + rect2.height / 2 + window.scrollY
      };

      const start = getEdgePoint(rect1, center2.x, center2.y);
      const end = getEdgePoint(rect2, center1.x, center1.y);

      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', start.x);
      line.setAttribute('y1', start.y);
      line.setAttribute('x2', end.x);
      line.setAttribute('y2', end.y);
      line.setAttribute('stroke', 'black');
      line.setAttribute('stroke-width', '1');
      line.setAttribute('marker-end', 'url(#arrow)');

      svg.appendChild(line);
    });
  });
}

function addArrowMarker() {
  const svg = document.getElementById('arrows');

  const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');

  const marker = document.createElementNS('http://www.w3.org/2000/svg', 'marker');
  marker.setAttribute('id', 'arrow');
  marker.setAttribute('markerWidth', '10');
  marker.setAttribute('markerHeight', '10');
  marker.setAttribute('refX', '10');
  marker.setAttribute('refY', '3');
  marker.setAttribute('orient', 'auto');
  marker.setAttribute('markerUnits', 'strokeWidth');

  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', 'M0,0 L0,6 L9,3 z');
  path.setAttribute('fill', 'black');

  marker.appendChild(path);
  defs.appendChild(marker);
  svg.appendChild(defs);
}

window.addEventListener('resize', drawArrows);
window.addEventListener('scroll', drawArrows);
window.addEventListener('load', drawArrows);