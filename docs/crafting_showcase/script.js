const recipes = [
  {
    id: "turret_wand",
    name: "召唤法杖 / Doom Turret Wand",
    badge: "正式配方",
    resultIcon: "icon-turret-wand",
    recipe: [
      null, "icon-rotten-flesh", null,
      null, "icon-obsidian", null,
      null, "icon-iron-block", null
    ],
    materials: [
      "腐肉 ×1",
      "黑曜石 ×1",
      "铁块 ×1"
    ],
    description: "核心部署道具。右键方块面可部署绑定你的骷髅炮塔单位。",
    conditions: "需要工作台；放置点必须可生成实体。"
  },
  {
    id: "summon_terminal",
    name: "召唤终端 / Summon Terminal",
    badge: "正式配方",
    resultIcon: "icon-summon-terminal",
    recipe: [
      null, null, null,
      "icon-redstone-torch", "icon-glowstone", "icon-redstone-torch",
      null, "icon-redstone-block", null
    ],
    materials: [
      "红石火把 ×2",
      "荧石 ×1",
      "红石块 ×1"
    ],
    description: "战术面板方块。用于队伍状态查看、召回、改名与集中管理。",
    conditions: "需要工作台；召回功能需要目标单位安装传送模块。"
  },
  {
    id: "teleport_module",
    name: "传送升级模块 / Teleport Module",
    badge: "正式配方",
    resultIcon: "icon-teleport-module",
    recipe: [
      null, "icon-lapis-block", null,
      null, "icon-ender-pearl", null,
      null, "icon-redstone-block", null
    ],
    materials: [
      "青金石块 ×1",
      "末影珍珠 ×1",
      "红石块 ×1"
    ],
    description: "解锁炮塔的传送与终端召回能力，可继续通过铁砧进行等级提升。",
    conditions: "需要工作台；安装后仍受冷却、安全落点等规则约束。"
  },
  {
    id: "multishot_module",
    name: "多重射击模块 / Multi-Shot Module",
    badge: "正式配方",
    resultIcon: "icon-multishot-module",
    recipe: [
      null, "icon-amethyst-shard", null,
      null, "icon-bow", null,
      null, "icon-string", null
    ],
    materials: [
      "紫水晶碎片 ×1",
      "弓 ×1",
      "线 ×1"
    ],
    description: "用于提升远程齐射能力，适合对群目标压制。",
    conditions: "需要工作台；可通过铁砧继续升级 Lv1-Lv5。"
  },
  {
    id: "core_module_example",
    name: "战术中枢模块（文档扩展示例）",
    badge: "示例配方",
    resultIcon: "icon-core-module",
    recipe: [
      "icon-redstone-block", "icon-ender-pearl", "icon-redstone-block",
      "icon-glowstone", "icon-teleport-module", "icon-glowstone",
      "icon-redstone-block", "icon-multishot-module", "icon-redstone-block"
    ],
    materials: [
      "红石块 ×4",
      "末影珍珠 ×1",
      "荧石 ×2",
      "传送模块 ×1",
      "多重射击模块 ×1"
    ],
    description: "用于展示第五个完整 9 宫格案例的文档扩展项，体现模块融合设计样式。",
    conditions: "当前版本默认数据包未注册该配方；如需实装请新增 `data/examplemod/recipes` 对应 JSON。"
  }
];

function iconSvg(iconId, className = "item-icon") {
  return `<svg class="${className}" viewBox="0 0 48 48" aria-hidden="true"><use href="./crafting_examples.svg#${iconId}"></use></svg>`;
}

function arrowSvg() {
  return '<svg class="arrow" viewBox="0 0 32 18" aria-hidden="true"><use href="./crafting_examples.svg#arrow"></use></svg>';
}

function buildCard(recipe, index) {
  const grid = recipe.recipe
    .map((slotIcon) => `<div class="slot">${slotIcon ? iconSvg(slotIcon) : ""}</div>`)
    .join("");

  const materialItems = recipe.materials.map((it) => `<li>${it}</li>`).join("");
  const detailsId = `details-${recipe.id}`;
  const expanded = index === 0;

  return `
    <article class="recipe-card">
      <div class="card-title-row">
        <h2 class="card-title">${recipe.name}</h2>
        <span class="badge">${recipe.badge}</span>
      </div>

      <div class="recipe-top">
        <div class="craft-layout" role="img" aria-label="${recipe.name} 合成九宫格与结果位">
          <div class="craft-grid">${grid}</div>
          ${arrowSvg()}
          <div class="result-slot">${iconSvg(recipe.resultIcon)}</div>
        </div>
      </div>

      <button class="toggle-btn" type="button" aria-expanded="${expanded}" aria-controls="${detailsId}">
        ${expanded ? "收起" : "展开"} 文字说明
      </button>

      <div id="${detailsId}" class="details-wrap" ${expanded ? "" : "hidden"}>
        <div class="details-scroll">
          <h4>物品名称</h4>
          <p>${recipe.name}</p>

          <h4>合成材料清单</h4>
          <ul>${materialItems}</ul>

          <h4>功能描述</h4>
          <p>${recipe.description}</p>

          <h4>使用条件</h4>
          <p>${recipe.conditions}</p>
          <p>该区域最大高度为 300px，超出后自动启用滚动。支持鼠标滚轮与拖拽滚动条操作。</p>
        </div>
      </div>
    </article>
  `;
}

function mount() {
  const container = document.getElementById("recipe-list");
  container.innerHTML = recipes.map(buildCard).join("");

  container.addEventListener("click", (event) => {
    const btn = event.target.closest(".toggle-btn");
    if (!btn) {
      return;
    }

    const detailsId = btn.getAttribute("aria-controls");
    const wrap = document.getElementById(detailsId);
    const next = btn.getAttribute("aria-expanded") !== "true";

    btn.setAttribute("aria-expanded", String(next));
    btn.textContent = `${next ? "收起" : "展开"} 文字说明`;
    wrap.hidden = !next;
  });
}

mount();
