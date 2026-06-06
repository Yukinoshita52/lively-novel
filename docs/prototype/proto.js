/* Lively Novel 原型 · 共享交互（仅为演示，非真实逻辑） */
(function () {
  // 接口场记板 折叠
  window.toggleDock = function (el) {
    el.closest('.api-dock').classList.toggle('collapsed');
  };

  document.addEventListener('click', function (e) {
    // 剧本类型卡选择
    var tc = e.target.closest('.type-card');
    if (tc && !tc.classList.contains('soon')) {
      tc.parentElement.querySelectorAll('.type-card').forEach(function (c) { c.classList.remove('on'); });
      tc.classList.add('on');
    }
    // 大纲场景选择
    var li = e.target.closest('.outline li');
    if (li) {
      li.parentElement.querySelectorAll('li').forEach(function (c) { c.classList.remove('on'); });
      li.classList.add('on');
    }
    // 标签页
    var tab = e.target.closest('.tab');
    if (tab) {
      var wrap = tab.closest('[data-tabs]');
      wrap.querySelectorAll('.tab').forEach(function (t) { t.classList.remove('on'); });
      tab.classList.add('on');
      var name = tab.dataset.tab;
      wrap.querySelectorAll('[data-pane]').forEach(function (p) {
        p.style.display = (p.dataset.pane === name) ? '' : 'none';
      });
    }
    // 登录/注册切换
    var sw = e.target.closest('[data-auth-switch]');
    if (sw) {
      e.preventDefault();
      document.querySelectorAll('[data-auth-form]').forEach(function (f) {
        f.style.display = (f.dataset.authForm === sw.dataset.authSwitch) ? '' : 'none';
      });
    }
  });

  // 转换页：伪 SSE 流
  window.startFakeStream = function () {
    var box = document.getElementById('stream');
    if (!box) return;
    var events = [
      ['analysis_start', '正在全局分析…（自适应：本篇 3 章走全文单次）'],
      ['analysis_progress', '提取人物与关系… 50%'],
      ['analysis_complete', '人物 4 · 场景 8 · 线索 2'],
      ['generation_start', '开始逐场生成（携带前场摘要保持连贯）…'],
      ['scene_generated', 'S1 内景·出租屋·夜'],
      ['scene_generated', 'S2 外景·天台·黄昏  ✦ 内心戏→V.O.'],
      ['scene_generated', 'S3 内景·公司·日'],
      ['complete', '剧本已生成 · screenplayId=sp_7f3a'],
    ];
    var phases = document.querySelectorAll('.phase-row');
    var i = 0;
    (function next() {
      if (i >= events.length) { box.querySelector('.caret') && box.querySelector('.caret').remove(); return; }
      var ev = events[i];
      var line = document.createElement('div');
      line.className = 'ev';
      line.innerHTML = 'event: <span class="k">' + ev[0] + '</span>\n  ' +
        (ev[0] === 'scene_generated' ? '<span class="s">' : '') + ev[1] + (ev[0] === 'scene_generated' ? '</span>' : '');
      var caret = box.querySelector('.caret');
      box.insertBefore(line, caret);
      box.scrollTop = box.scrollHeight;
      // 阶段灯
      if (ev[0] === 'analysis_start') setPhase(phases, 0);
      if (ev[0] === 'generation_start') setPhase(phases, 1);
      if (ev[0] === 'complete') setPhase(phases, 2);
      i++;
      setTimeout(next, 850);
    })();
  };
  function setPhase(phases, idx) {
    phases.forEach(function (p, n) {
      p.classList.remove('active');
      if (n < idx) p.classList.add('done');
      if (n === idx) p.classList.add('active');
    });
  }
  document.addEventListener('DOMContentLoaded', function () {
    if (document.getElementById('stream')) setTimeout(window.startFakeStream, 500);
  });
})();
