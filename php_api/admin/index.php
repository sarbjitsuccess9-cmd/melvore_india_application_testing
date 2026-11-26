<?php
declare(strict_types=1);
?><!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>Melvore Creative Studio — Worlds, Luxe & Categories Admin</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <style>
    body { font-family: ui-sans-serif, system-ui, -apple-system, sans-serif; background: #0B0B0F; color: #F4F4F5; }
    .phone-frame { width: 320px; border-radius: 32px; border: 6px solid #27272A; overflow: hidden; box-shadow: 0 24px 60px rgba(0,0,0,.6); }
    .mono { font-family: ui-monospace, SFMono-Regular, monospace; }
  </style>
</head>
<body class="min-h-screen">
<div id="app" class="max-w-7xl mx-auto p-4 md:p-6">
  <!-- Header -->
  <header class="flex flex-wrap items-center justify-between gap-4 border-b border-zinc-800 pb-4 mb-6">
    <div>
      <p class="text-xs uppercase tracking-[0.25em] text-amber-400">Melvore Editorial CMS</p>
      <h1 class="text-2xl font-bold tracking-tight">Fashion Worlds, Luxe & Category Studio</h1>
    </div>
    <div id="authStatus" class="flex items-center gap-3"></div>
  </header>

  <!-- Login Panel -->
  <section id="loginPanel" class="max-w-md mx-auto bg-zinc-900 border border-zinc-800 rounded-2xl p-6 hidden">
    <h2 class="text-lg font-semibold mb-1">Administrator Sign In</h2>
    <p class="text-xs text-zinc-400 mb-4">Sign in with an active admin or merchandiser account (`users.role`).</p>
    <div class="space-y-3">
      <input id="loginEmail" type="email" placeholder="Admin email" class="w-full rounded-lg bg-zinc-950 border border-zinc-700 px-3 py-2 text-sm" />
      <input id="loginPass" type="password" placeholder="Password" class="w-full rounded-lg bg-zinc-950 border border-zinc-700 px-3 py-2 text-sm" />
      <button onclick="adminLogin()" class="w-full bg-amber-500 hover:bg-amber-400 text-zinc-950 font-semibold rounded-lg py-2.5 text-sm">Sign In to Studio</button>
      <p id="loginErr" class="text-xs text-red-400 hidden"></p>
    </div>
  </section>

  <!-- Main Workspace -->
  <main id="workspace" class="hidden">
    <div class="flex flex-wrap gap-2 mb-6 items-center">
      <button id="tab-design" onclick="switchTab('design')" class="px-4 py-2 rounded-lg text-sm font-semibold bg-amber-500 text-zinc-950">Design System Studio</button>
      <button id="tab-occasions" onclick="switchTab('occasions')" class="px-4 py-2 rounded-lg text-sm font-semibold bg-zinc-800 text-zinc-200">Occasions &amp; Simulator</button>
      <button id="tab-worlds" onclick="switchTab('worlds')" class="px-4 py-2 rounded-lg text-sm font-semibold bg-zinc-800 text-zinc-200">Fashion Worlds</button>
      <button id="tab-luxe" onclick="switchTab('luxe')" class="px-4 py-2 rounded-lg text-sm font-semibold bg-zinc-800 text-zinc-200">Luxe Collections</button>
      <button id="tab-categories" onclick="switchTab('categories')" class="px-4 py-2 rounded-lg text-sm font-semibold bg-zinc-800 text-zinc-200">Homepage Categories</button>
      <span id="toast" class="ml-auto text-xs px-3 py-2 rounded-lg bg-emerald-900/60 text-emerald-300 hidden"></span>
    </div>

    <!-- DESIGN SYSTEM STUDIO TAB -->
    <section id="panel-design" class="space-y-6">
      <!-- Top Control Strip: 0/1 Switch, Emergency Rollback, Save Draft, Publish -->
      <div class="bg-zinc-900 border border-zinc-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-4">
          <div>
            <p class="text-[10px] uppercase tracking-[0.2em] text-zinc-400">Global Active Storefront Flag (`active_design`)</p>
            <div class="flex items-center gap-2 mt-1">
              <span id="dsActiveBadge" class="px-2.5 py-1 rounded-full text-xs font-bold bg-zinc-800 text-amber-400">0 — DEFAULT</span>
              <button onclick="confirmSwitchDesign(0)" class="px-3 py-1 rounded-lg text-xs font-semibold bg-zinc-800 hover:bg-zinc-700">Set Default (0)</button>
              <button onclick="confirmSwitchDesign(1)" class="px-3 py-1 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white">Activate Live (1)</button>
              <button onclick="rollbackToDefault()" class="px-3 py-1 rounded-lg text-xs font-semibold bg-red-900/70 hover:bg-red-800 text-red-100">Instant Rollback &rarr; 0</button>
            </div>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <span class="text-xs text-zinc-400">Editing Slot:</span>
          <button id="slotBtnDefault" onclick="selectDesignSlot('default')" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-zinc-800 text-zinc-200">Default (`0`)</button>
          <button id="slotBtnLive" onclick="selectDesignSlot('live')" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-500 text-zinc-950">Live (`1`)</button>
          <button onclick="loadDraftIntoEditor()" class="px-3 py-1.5 rounded-lg text-xs bg-zinc-800 hover:bg-zinc-700">Load Latest Draft</button>
          <button onclick="saveDesignDraft()" class="px-3 py-1.5 rounded-lg text-xs font-semibold bg-zinc-700 hover:bg-zinc-600">Save Draft</button>
          <button onclick="publishDesignSlot()" class="px-4 py-1.5 rounded-lg text-xs font-bold bg-amber-500 hover:bg-amber-400 text-zinc-950">Publish Release</button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Left Editor Column -->
        <div class="lg:col-span-8 space-y-5">
          <!-- Sub-tabs inside Design Studio -->
          <div class="flex flex-wrap gap-2 text-xs">
            <button onclick="switchDesignSubtab('tokens')" id="dsub-tokens" class="px-3 py-1.5 rounded-lg bg-amber-500 text-zinc-950 font-semibold">1. Tokens &amp; Type</button>
            <button onclick="switchDesignSubtab('nav')" id="dsub-nav" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">2. Navigation &amp; Icons</button>
            <button onclick="switchDesignSubtab('components')" id="dsub-components" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">3. Buttons &amp; Cards</button>
            <button onclick="switchDesignSubtab('pages')" id="dsub-pages" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">4. Page Section Composer</button>
            <button onclick="switchDesignSubtab('releases')" id="dsub-releases" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">5. Releases, Diff &amp; Rollback</button>
            <button onclick="switchDesignSubtab('assets')" id="dsub-assets" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">6. Asset Library</button>
            <button onclick="switchDesignSubtab('json')" id="dsub-json" class="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold">7. Raw Manifest JSON</button>
          </div>

          <!-- Subtab 1: Tokens -->
          <div id="ds-sub-tokens" class="bg-zinc-900 border border-zinc-800 rounded-2xl p-5 grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
            <label>Version Tag<input id="ds_version" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Mode
              <select id="ds_mode" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="light">light</option>
                <option value="dark">dark</option>
              </select>
            </label>
            <label>Display Font
              <select id="ds_disp_font" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="editorial_serif">editorial_serif (Playfair)</option>
                <option value="editorial_sans">editorial_sans (Jost)</option>
                <option value="futuristic">futuristic (Space Grotesk)</option>
                <option value="gothic">gothic (Cinzel)</option>
                <option value="minimal">minimal (Didact)</option>
                <option value="pixel">pixel (Silkscreen)</option>
              </select>
            </label>
            <label>Body Font
              <select id="ds_body_font" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="clean_sans">clean_sans (Plus Jakarta)</option>
                <option value="minimal">minimal (Didact)</option>
                <option value="editorial_sans">editorial_sans (Jost)</option>
              </select>
            </label>
            <label>Background Hex<input id="ds_bg" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Surface Hex<input id="ds_surface" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Primary Hex<input id="ds_primary" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Accent Hex<input id="ds_accent" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Text Primary Hex<input id="ds_text" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Border Hex<input id="ds_border" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Card Radius (dp)<input id="ds_card_radius" type="number" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Motion Intensity
              <select id="ds_motion" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="subtle">subtle</option>
                <option value="expressive">expressive</option>
                <option value="none">none</option>
              </select>
            </label>
            <label class="col-span-2 md:col-span-4">Release Notes<input id="ds_notes" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="syncDesignFromInputs()"/></label>
          </div>

          <!-- Subtab 2: Navigation & Icons -->
          <div id="ds-sub-nav" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-4 text-xs">
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
              <label>Top Bar Style
                <select id="ds_top_style" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                  <option value="editorial_masthead">editorial_masthead</option>
                  <option value="centered_monogram">centered_monogram</option>
                  <option value="compact_atelier">compact_atelier</option>
                </select>
              </label>
              <label>Top Title<input id="ds_top_title" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
              <label>Top Subtitle<input id="ds_top_subtitle" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
              <label>Bottom Nav Variant
                <select id="ds_nav_variant" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                  <option value="flat_editorial">flat_editorial</option>
                  <option value="floating_dock">floating_dock</option>
                  <option value="pill_minimal">pill_minimal</option>
                </select>
              </label>
            </div>
            <div>
              <p class="text-[11px] font-semibold text-zinc-300 mb-2">Bottom Bar Tabs (Label, Icon Key, Badge &amp; Visibility)</p>
              <div id="dsNavTabsEditor" class="space-y-2"></div>
            </div>
          </div>

          <!-- Subtab 3: Components -->
          <div id="ds-sub-components" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 grid grid-cols-2 md:grid-cols-3 gap-3 text-xs">
            <label>Button Variant
              <select id="ds_btn_variant" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="editorial_sharp">editorial_sharp</option>
                <option value="pill_luxe">pill_luxe</option>
                <option value="outlined_monogram">outlined_monogram</option>
                <option value="hardware_cut">hardware_cut</option>
                <option value="cloud_soft">cloud_soft</option>
              </select>
            </label>
            <label>Button Radius (dp)<input id="ds_btn_radius" type="number" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Search Bar Variant
              <select id="ds_search_variant" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="pill_elevated">pill_elevated</option>
                <option value="underlined_editorial">underlined_editorial</option>
                <option value="sharp_box">sharp_box</option>
                <option value="glass_dock">glass_dock</option>
              </select>
            </label>
            <label class="col-span-2">Search Placeholder<input id="ds_search_ph" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Product Card Variant
              <select id="ds_card_variant" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="editorial_portrait">editorial_portrait</option>
                <option value="gallery_bordered">gallery_bordered</option>
                <option value="compact_lookbook">compact_lookbook</option>
                <option value="borderless_magazine">borderless_magazine</option>
              </select>
            </label>
            <label>Card Image Aspect Ratio<input id="ds_card_aspect" type="number" step="0.02" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
            <label>Category Strip Variant
              <select id="ds_cat_variant" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="syncDesignFromInputs()">
                <option value="circular_story">circular_story</option>
                <option value="portrait_tiles">portrait_tiles</option>
                <option value="sharp_pills">sharp_pills</option>
                <option value="asymmetric_bento">asymmetric_bento</option>
              </select>
            </label>
            <label>Empty State Title<input id="ds_empty_title" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" oninput="syncDesignFromInputs()"/></label>
          </div>

          <!-- Subtab 4: Page Section Composer -->
          <div id="ds-sub-pages" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 text-xs">
            <div class="flex flex-wrap items-center gap-3">
              <label>Page:
                <select id="dsPageSelect" onchange="renderPageComposer()" class="ml-1 bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5">
                  <option value="home">home</option>
                  <option value="style_worlds">style_worlds</option>
                  <option value="categories">categories</option>
                  <option value="search">search</option>
                  <option value="product_details">product_details</option>
                  <option value="cart">cart</option>
                </select>
              </label>
              <label>Hero Height (dp): <input id="dsPageHeroH" type="number" class="w-20 bg-zinc-950 border border-zinc-700 rounded px-2 py-1" onchange="updatePageMeta()"/></label>
              <label>Grid Columns: <input id="dsPageCols" type="number" min="1" max="3" class="w-16 bg-zinc-950 border border-zinc-700 rounded px-2 py-1" onchange="updatePageMeta()"/></label>
            </div>
            <div id="dsSectionsList" class="space-y-2 max-h-96 overflow-y-auto"></div>
          </div>

          <!-- Subtab 5: Releases, Diff & Rollback -->
          <div id="ds-sub-releases" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-4 text-xs">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h4 class="font-semibold text-zinc-200">Published &amp; Draft Release History</h4>
              <div class="flex items-center gap-2">
                <input id="cmpLeft" type="number" placeholder="Left ID" class="w-20 bg-zinc-950 border border-zinc-700 rounded px-2 py-1"/>
                <input id="cmpRight" type="number" placeholder="Right ID" class="w-20 bg-zinc-950 border border-zinc-700 rounded px-2 py-1"/>
                <button onclick="compareTwoReleases()" class="px-3 py-1 rounded bg-zinc-800 hover:bg-zinc-700">Compare Diff</button>
              </div>
            </div>
            <div id="dsReleasesList" class="space-y-1.5 max-h-60 overflow-y-auto"></div>
            <pre id="dsDiffOutput" class="hidden mono bg-zinc-950 border border-zinc-800 rounded-xl p-3 text-[11px] max-h-56 overflow-auto"></pre>
          </div>

          <!-- Subtab 6: Assets -->
          <div id="ds-sub-assets" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 text-xs">
            <div class="flex flex-wrap items-center gap-2">
              <select id="dsAssetType" class="bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5">
                <option value="image">image</option>
                <option value="icon">icon</option>
                <option value="illustration">illustration</option>
                <option value="animation">animation</option>
                <option value="sound">sound</option>
              </select>
              <input id="dsAssetKey" placeholder="asset_key (snake_case)" class="bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/>
              <input id="dsAssetFile" type="file" class="text-zinc-400"/>
              <button onclick="uploadDesignAsset()" class="px-3 py-1.5 rounded bg-amber-500 text-zinc-950 font-semibold">Upload Asset</button>
            </div>
            <div id="dsAssetsList" class="grid grid-cols-1 md:grid-cols-2 gap-2 max-h-64 overflow-y-auto"></div>
          </div>

          <!-- Subtab 7: Raw JSON -->
          <div id="ds-sub-json" class="hidden bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-2 text-xs">
            <textarea id="dsRawJson" rows="18" class="mono w-full bg-zinc-950 border border-zinc-700 rounded-xl p-3 text-[11px]"></textarea>
            <button onclick="applyRawJsonToEditor()" class="px-4 py-1.5 rounded bg-zinc-800 hover:bg-zinc-700">Apply JSON to Visual Editor</button>
          </div>
        </div>

        <!-- Live Design Studio Mobile Preview -->
        <div class="lg:col-span-4 flex flex-col items-center">
          <p class="text-xs text-zinc-400 mb-2">Live Storefront Preview (<span id="dsPreviewSlotLabel">live</span>)</p>
          <div id="designPhonePreview" class="phone-frame min-h-[540px] flex flex-col justify-between"></div>
        </div>
      </div>
    </section>

    <!-- OCCASIONS & SIMULATOR TAB -->
    <section id="panel-occasions" class="hidden space-y-6">
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <!-- Saved Occasions List -->
        <div class="lg:col-span-3 bg-zinc-900 border border-zinc-800 rounded-2xl p-4 space-y-2">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-semibold">Occasions</h3>
            <button onclick="newOccasion()" class="text-xs px-2 py-1 rounded bg-amber-500 text-zinc-950 font-semibold">+ New</button>
          </div>
          <div id="occasionsList" class="space-y-1.5 max-h-[520px] overflow-y-auto"></div>
        </div>

        <!-- Occasion Form (auto 8-day-before IST scheduler) -->
        <div class="lg:col-span-5 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 text-xs">
          <h3 class="text-sm font-semibold uppercase tracking-wider text-zinc-400">Automatic 8-Day-Before Scheduler</h3>
          <div class="grid grid-cols-2 gap-3">
            <label>Slug<input id="o_slug" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Name<input id="o_name" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Eyebrow<input id="o_eyebrow" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Headline<input id="o_headline" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label class="col-span-2">Subtitle<input id="o_subtitle" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Event Date (YYYY-MM-DD)<input id="o_event_date" type="date" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="recomputeOccasionWindowPreview()"/></label>
            <label>Days Before Activation<input id="o_days_before" type="number" value="8" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="recomputeOccasionWindowPreview()"/></label>
            <label>Timezone<input id="o_tz" value="Asia/Kolkata" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Priority<input id="o_priority" type="number" value="80" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Country<input id="o_country" value="India" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>State (optional)<input id="o_state" placeholder="e.g. Maharashtra" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Accent Hex<input id="o_accent" value="#E6B34B" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>Background Hex<input id="o_bg" value="#1B1224" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label class="flex items-center gap-2 pt-3"><input id="o_active" type="checkbox" checked/> Active Flag (`is_active`)</label>
          </div>
          <div id="o_window_preview" class="bg-zinc-950 border border-zinc-800 rounded-lg p-2.5 text-[11px] text-amber-300"></div>
          <button onclick="saveOccasion()" class="w-full bg-amber-500 hover:bg-amber-400 text-zinc-950 font-semibold rounded-lg py-2.5 text-sm">Save Occasion Schedule</button>
        </div>

        <!-- Admin Occasion Date & Blend Simulator -->
        <div class="lg:col-span-4 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 text-xs">
          <h3 class="text-sm font-semibold uppercase tracking-wider text-zinc-400">Occasion Date Simulator (Admin Only)</h3>
          <p class="text-[11px] text-zinc-400">Preview which occasion triggers at any timestamp in IST without modifying live customer traffic.</p>
          <div class="grid grid-cols-2 gap-2">
            <label class="col-span-2">Simulated IST Datetime<input id="sim_dt" value="2026-10-12 00:05:00" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5 mono"/></label>
            <label>Country<input id="sim_country" value="India" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label>State<input id="sim_state" value="Maharashtra" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5"/></label>
            <label class="col-span-2">Blend Inside Style World
              <select id="sim_world" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5">
                <option value="">Storefront Home</option>
                <option value="y2k">y2k</option>
                <option value="goth">goth</option>
                <option value="coquette">coquette</option>
                <option value="streetwear">streetwear</option>
                <option value="k-fashion">k-fashion</option>
              </select>
            </label>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <button onclick="runSimPreset('2026-10-11 23:55:00')" class="px-2 py-1 rounded bg-zinc-800">Oct 11 (-9d)</button>
            <button onclick="runSimPreset('2026-10-12 00:05:00')" class="px-2 py-1 rounded bg-zinc-800">Oct 12 (-8d)</button>
            <button onclick="runSimPreset('2026-10-16 14:00:00')" class="px-2 py-1 rounded bg-zinc-800">Oct 16 (-4d)</button>
            <button onclick="runSimPreset('2026-10-20 18:00:00')" class="px-2 py-1 rounded bg-zinc-800">Oct 20 (Event)</button>
            <button onclick="runSimPreset('2026-10-21 00:05:00')" class="px-2 py-1 rounded bg-zinc-800">Oct 21 (+1d)</button>
          </div>
          <button onclick="runOccasionSimulation()" class="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-lg py-2">Simulate Resolution</button>
          <div id="simOutput" class="space-y-2 max-h-64 overflow-y-auto"></div>
        </div>
      </div>
    </section>

    <!-- WORLDS TAB -->
    <section id="panel-worlds" class="hidden grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-3 bg-zinc-900 border border-zinc-800 rounded-2xl p-4 space-y-2">
        <div class="flex items-center justify-between mb-2">
          <h3 class="text-sm font-semibold">Worlds</h3>
          <button onclick="newWorld()" class="text-xs px-2 py-1 rounded bg-zinc-800 hover:bg-zinc-700">+ New</button>
        </div>
        <div id="worldsList" class="space-y-1.5"></div>
      </div>

      <div class="lg:col-span-5 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-4">
        <h3 class="text-sm font-semibold uppercase tracking-wider text-zinc-400">World Visuals, Audio & Video</h3>
        <div class="grid grid-cols-2 gap-3 text-xs">
          <label>Slug<input id="w_slug" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label>Display Name<input id="w_name" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label class="col-span-2">Tagline<input id="w_tagline" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label>Background Hex<input id="w_bg" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label>Primary Hex<input id="w_primary" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label>Accent Hex<input id="w_accent" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderWorldPreview()"/></label>
          <label>Font Style
            <select id="w_font" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="renderWorldPreview()">
              <option value="pixel">pixel (Y2K Silkscreen)</option>
              <option value="gothic">gothic (Cinzel Noir)</option>
              <option value="editorial_serif">editorial_serif (Coquette/Luxury)</option>
              <option value="futuristic">futuristic (Space Grotesk)</option>
              <option value="minimal_grotesk">minimal_grotesk (K-Fashion)</option>
            </select>
          </label>
          <label>Button Style
            <select id="w_button" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="renderWorldPreview()">
              <option value="y2k_pixel_chrome">y2k_pixel_chrome</option>
              <option value="goth_fang">goth_fang</option>
              <option value="coquette_ribbon">coquette_ribbon</option>
              <option value="streetwear_utility">streetwear_utility</option>
              <option value="kfashion_minimal">kfashion_minimal</option>
              <option value="halloween_pumpkin">halloween_pumpkin</option>
              <option value="diwali_gold">diwali_gold</option>
            </select>
          </label>
          <label>Animation Key<input id="w_anim" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <label>Audio URL (MP3/M4A)<input id="w_audio_url" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <div class="flex items-end gap-2">
            <label class="flex-1">Audio Vol (0.05-1)<input id="w_audio_vol" type="number" step="0.05" min="0.05" max="1" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
            <label class="flex items-center gap-1 pb-2"><input id="w_audio_loop" type="checkbox" checked/> Loop</label>
          </div>
          <label class="col-span-2">Upload Audio File<input type="file" accept="audio/*" onchange="uploadAsset(this,'audio','worlds_audio','w_audio_url')" class="mt-1 block w-full text-zinc-400"/></label>
          <label class="col-span-2">Promo Video URL (MP4)<input id="w_promo_url" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <label>Promo Poster URL<input id="w_poster_url" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <label class="flex items-center gap-2 pt-4"><input id="w_promo_sponsored" type="checkbox"/> Sponsored Video</label>
          <label class="col-span-2">Runway Config (JSON — Y2K models/preset/speed/position)
            <textarea id="w_runway_json" rows="5" class="mono mt-1 w-full bg-zinc-950 border border-zinc-700 rounded p-2 text-[11px]" oninput="renderWorldPreview()"></textarea>
          </label>
        </div>
        <button onclick="saveWorld()" class="w-full bg-amber-500 hover:bg-amber-400 text-zinc-950 font-semibold rounded-lg py-2.5 text-sm">Save World Configuration</button>
      </div>

      <div class="lg:col-span-4 flex flex-col items-center">
        <p class="text-xs text-zinc-400 mb-2">Live Mobile World Preview</p>
        <div id="worldPreview" class="phone-frame p-4 space-y-3 min-h-[480px]"></div>
      </div>
    </section>

    <!-- LUXE TAB -->
    <section id="panel-luxe" class="hidden grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-3 bg-zinc-900 border border-zinc-800 rounded-2xl p-4 space-y-2">
        <div class="flex items-center justify-between mb-2">
          <h3 class="text-sm font-semibold">Luxe Collections</h3>
          <button onclick="newLuxePack()" class="text-xs px-2 py-1 rounded bg-amber-500 text-zinc-950 font-semibold">+ Create</button>
        </div>
        <div id="luxeList" class="space-y-1.5"></div>
      </div>

      <div class="lg:col-span-5 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-sm font-semibold uppercase tracking-wider text-zinc-400">Collection Details & Curated Products</h3>
          <div class="flex gap-2">
            <button onclick="duplicateLuxePack()" class="text-xs px-2.5 py-1 rounded bg-zinc-800 hover:bg-zinc-700">Duplicate</button>
            <button onclick="archiveLuxePack()" class="text-xs px-2.5 py-1 rounded bg-red-900/60 text-red-200">Archive</button>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3 text-xs">
          <label>Slug<input id="l_slug" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <label>Title<input id="l_title" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderLuxePreview()"/></label>
          <label>Brand / House<input id="l_brand" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderLuxePreview()"/></label>
          <label>Layout Preset
            <select id="l_layout" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5">
              <option value="editorial_grid">editorial_grid</option>
              <option value="hero_spotlight">hero_spotlight</option>
              <option value="portrait_diptych">portrait_diptych</option>
              <option value="showcase_grid">showcase_grid</option>
            </select>
          </label>
          <label class="col-span-2">Subtitle<input id="l_subtitle" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderLuxePreview()"/></label>
          <label class="col-span-2">Story<textarea id="l_story" rows="2" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded p-2"></textarea></label>
          <label>Collection Banner URL<input id="l_hero" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderLuxePreview()"/></label>
          <label>Status
            <select id="l_status" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5">
              <option value="published">Published</option>
              <option value="draft">Draft</option>
              <option value="archived">Archived</option>
            </select>
          </label>
          <label>Accent Hex<input id="l_accent" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderLuxePreview()"/></label>
          <div class="flex items-center gap-4 pt-4">
            <label class="flex items-center gap-1"><input id="l_is_hero" type="checkbox"/> Hero Carousel</label>
            <label class="flex items-center gap-1"><input id="l_is_sponsored" type="checkbox"/> Sponsored</label>
          </div>
        </div>

        <div class="border-t border-zinc-800 pt-3 space-y-2">
          <div class="flex gap-2">
            <input id="prodSearchInput" placeholder="Search real catalog products by name/SKU..." class="flex-1 bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-xs" onkeyup="searchCatalog()"/>
          </div>
          <div id="prodSearchResults" class="max-h-36 overflow-y-auto space-y-1"></div>
          <h4 class="text-xs font-semibold text-zinc-300 pt-2">Assigned Products (each keeps its own catalog image)</h4>
          <div id="luxeItemsContainer" class="space-y-1.5 max-h-56 overflow-y-auto"></div>
        </div>

        <button onclick="saveLuxePack()" class="w-full bg-amber-500 hover:bg-amber-400 text-zinc-950 font-semibold rounded-lg py-2.5 text-sm">Save Luxe Collection</button>
      </div>

      <div class="lg:col-span-4 flex flex-col items-center">
        <p class="text-xs text-zinc-400 mb-2">Live Luxe Mobile Preview</p>
        <div id="luxePreview" class="phone-frame bg-black p-4 space-y-3 min-h-[480px]"></div>
      </div>
    </section>

    <!-- CATEGORIES TAB -->
    <section id="panel-categories" class="hidden grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-4 bg-zinc-900 border border-zinc-800 rounded-2xl p-4 space-y-2">
        <h3 class="text-sm font-semibold mb-2">Homepage Category Editorial Index</h3>
        <div id="catsList" class="space-y-1.5"></div>
      </div>
      <div class="lg:col-span-4 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 text-xs">
        <h3 class="text-sm font-semibold uppercase tracking-wider text-zinc-400">Editorial Showcase Config</h3>
        <label class="block">Badge<input id="c_badge" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderCatPreview()"/></label>
        <label class="block">Subtitle<input id="c_subtitle" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderCatPreview()"/></label>
        <div class="grid grid-cols-2 gap-3">
          <label>Motif
            <select id="c_motif" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="renderCatPreview()">
              <option value="editorial_arch">editorial_arch</option>
              <option value="chrome_spark">chrome_spark</option>
              <option value="ribbon_crest">ribbon_crest</option>
              <option value="noir_stencil">noir_stencil</option>
              <option value="minimal_grid">minimal_grid</option>
            </select>
          </label>
          <label>Tile Span
            <select id="c_span" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2 py-1.5" onchange="renderCatPreview()">
              <option value="feature">feature (Hero Asymmetric)</option>
              <option value="wide">wide (Editorial Strip)</option>
              <option value="regular">regular</option>
            </select>
          </label>
          <label>Accent Hex<input id="c_accent" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderCatPreview()"/></label>
          <label>Card Surface Hex<input id="c_bg" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5" oninput="renderCatPreview()"/></label>
          <label>Order<input id="c_order" type="number" class="mt-1 w-full bg-zinc-950 border border-zinc-700 rounded px-2.5 py-1.5"/></label>
          <label class="flex items-center gap-2 pt-5"><input id="c_active" type="checkbox" checked/> Visible</label>
        </div>
        <button onclick="saveCategoryConfig()" class="w-full bg-amber-500 hover:bg-amber-400 text-zinc-950 font-semibold rounded-lg py-2.5 text-sm">Save Category Showcase</button>
      </div>
      <div class="lg:col-span-4 flex flex-col items-center">
        <p class="text-xs text-zinc-400 mb-2">Live Editorial Card Preview</p>
        <div id="catPreview" class="phone-frame bg-white text-zinc-900 p-4 min-h-[280px]"></div>
      </div>
    </section>
  </main>
</div>

<script>
const API_BASE = '../api';
let token = sessionStorage.getItem('mlv_admin_token') || '';
let worlds = [], currentWorld = null;
let luxePacks = [], currentLuxe = null;
let categories = [], currentCat = null;

function toast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 3000);
}

async function api(path, opts = {}) {
  const headers = opts.headers || {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (!(opts.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  const res = await fetch(API_BASE + path, { ...opts, headers });
  const data = await res.json();
  if (!res.ok || data.success === false) {
    throw new Error(data.message || 'API error');
  }
  return data.data || data;
}

function initAuth() {
  if (!token) {
    document.getElementById('loginPanel').classList.remove('hidden');
    document.getElementById('workspace').classList.add('hidden');
    document.getElementById('authStatus').innerHTML = '';
  } else {
    document.getElementById('loginPanel').classList.add('hidden');
    document.getElementById('workspace').classList.remove('hidden');
    document.getElementById('authStatus').innerHTML = '<button onclick="logout()" class="text-xs px-3 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700">Sign Out</button>';
    loadAll();
  }
}

async function adminLogin() {
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPass').value;
  try {
    const res = await fetch(API_BASE + '/auth/login.php', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({ email, password })
    }).then(r => r.json());
    if (!res.token) throw new Error(res.message || 'Login failed');
    token = res.token;
    sessionStorage.setItem('mlv_admin_token', token);
    initAuth();
  } catch (e) {
    const err = document.getElementById('loginErr');
    err.textContent = e.message;
    err.classList.remove('hidden');
  }
}

function logout() { sessionStorage.removeItem('mlv_admin_token'); token = ''; initAuth(); }

let designWorkspace = null;
let editingSlot = 'live';
let currentManifest = null;
let occasionsList = [], currentOccasion = null;

function switchTab(tab) {
  ['design', 'occasions', 'worlds', 'luxe', 'categories'].forEach(t => {
    document.getElementById('panel-' + t).classList.toggle('hidden', t !== tab);
    document.getElementById('tab-' + t).className = t === tab
      ? 'px-4 py-2 rounded-lg text-sm font-semibold bg-amber-500 text-zinc-950'
      : 'px-4 py-2 rounded-lg text-sm font-semibold bg-zinc-800 text-zinc-200';
  });
}

async function loadAll() {
  try {
    const [dRes, oRes, wRes, lRes, cRes] = await Promise.all([
      api('/admin/design.php'),
      api('/admin/occasions.php'),
      api('/admin/worlds.php'),
      api('/admin/elite.php'),
      api('/admin/categories.php')
    ]);
    designWorkspace = dRes;
    occasionsList = oRes.occasions || [];
    worlds = wRes.worlds || [];
    luxePacks = lRes.packs || [];
    categories = cRes.categories || [];
    initDesignStudio();
    renderOccasionsList();
    if (occasionsList.length) selectOccasion(occasionsList[0]);
    renderWorldsList();
    if (worlds.length) selectWorld(worlds[0]);
    renderLuxeList();
    if (luxePacks.length) selectLuxe(luxePacks[0]);
    renderCatsList();
    if (categories.length) selectCat(categories[0]);
  } catch (e) { alert('Load error (ensure admin role): ' + e.message); }
}

function initDesignStudio() {
  if (!designWorkspace) return;
  const flag = Number(designWorkspace.active_design || 0);
  const badge = document.getElementById('dsActiveBadge');
  badge.textContent = flag === 1 ? '1 — LIVE STOREFRONT ACTIVE' : '0 — DEFAULT ACTIVE';
  badge.className = flag === 1
    ? 'px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-900/70 text-emerald-300'
    : 'px-2.5 py-1 rounded-full text-xs font-bold bg-zinc-800 text-amber-400';
  selectDesignSlot(editingSlot);
  renderDesignReleases();
  renderDesignAssets();
}

function selectDesignSlot(slot) {
  editingSlot = slot === 'default' ? 'default' : 'live';
  document.getElementById('slotBtnDefault').className = editingSlot === 'default'
    ? 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-500 text-zinc-950'
    : 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-zinc-800 text-zinc-200';
  document.getElementById('slotBtnLive').className = editingSlot === 'live'
    ? 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-500 text-zinc-950'
    : 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-zinc-800 text-zinc-200';
  document.getElementById('dsPreviewSlotLabel').textContent = editingSlot;
  const src = editingSlot === 'live' ? designWorkspace.live_manifest : designWorkspace.default_manifest;
  currentManifest = JSON.parse(JSON.stringify(src || {}));
  populateDesignInputs();
}

function loadDraftIntoEditor() {
  const draft = editingSlot === 'live' ? designWorkspace.live_draft : designWorkspace.default_draft;
  if (!draft) return toast('No unpublished draft found for ' + editingSlot);
  currentManifest = JSON.parse(JSON.stringify(draft));
  populateDesignInputs();
  toast('Loaded latest draft for ' + editingSlot);
}

function switchDesignSubtab(sub) {
  ['tokens', 'nav', 'components', 'pages', 'releases', 'assets', 'json'].forEach(k => {
    document.getElementById('ds-sub-' + k).classList.toggle('hidden', k !== sub);
    document.getElementById('dsub-' + k).className = k === sub
      ? 'px-3 py-1.5 rounded-lg bg-amber-500 text-zinc-950 font-semibold'
      : 'px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 font-semibold';
  });
}

function populateDesignInputs() {
  if (!currentManifest) return;
  const c = currentManifest.tokens?.colors || {};
  const t = currentManifest.tokens?.typography || {};
  const r = currentManifest.tokens?.radii || {};
  const m = currentManifest.tokens?.motion || {};
  const nav = currentManifest.navigation || {};
  const comp = currentManifest.components || {};

  ds_version.value = currentManifest.design_version || '1.0.0';
  ds_mode.value = c.mode || 'light';
  ds_disp_font.value = t.display_font || 'editorial_serif';
  ds_body_font.value = t.body_font || 'clean_sans';
  ds_bg.value = c.background || '#FFFFFF';
  ds_surface.value = c.surface || '#F7F7FA';
  ds_primary.value = c.primary || '#09090B';
  ds_accent.value = c.accent || '#C9A24B';
  ds_text.value = c.text_primary || '#09090B';
  ds_border.value = c.border || '#E4E4E7';
  ds_card_radius.value = r.card_dp ?? 6;
  ds_motion.value = m.intensity || 'subtle';
  ds_notes.value = currentManifest.release_notes || '';

  ds_top_style.value = nav.top_bar_style || 'editorial_masthead';
  ds_top_title.value = nav.top_bar_title || 'MELVORE';
  ds_top_subtitle.value = nav.top_bar_subtitle || '';
  ds_nav_variant.value = nav.bottom_bar_variant || 'flat_editorial';
  renderNavTabsEditor();

  ds_btn_variant.value = comp.button?.primary_variant || 'editorial_sharp';
  ds_btn_radius.value = comp.button?.corner_radius_dp ?? 4;
  ds_search_variant.value = comp.search_bar?.variant || 'pill_elevated';
  ds_search_ph.value = comp.search_bar?.placeholder || '';
  ds_card_variant.value = comp.product_card?.variant || 'editorial_portrait';
  ds_card_aspect.value = comp.product_card?.image_aspect_ratio ?? 0.78;
  ds_cat_variant.value = comp.category_strip?.variant || 'circular_story';
  ds_empty_title.value = comp.states?.empty_title || 'Nothing Curated Here Yet';

  renderPageComposer();
  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderDesignPhonePreview();
}

function renderNavTabsEditor() {
  const tabs = currentManifest.navigation?.tabs || [];
  document.getElementById('dsNavTabsEditor').innerHTML = tabs.map((tb, idx) => `
    <div class="grid grid-cols-5 gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-2 items-center">
      <span class="font-semibold text-amber-400">${tb.route}</span>
      <input value="${tb.label||''}" onchange="updateNavTab(${idx},'label',this.value)" placeholder="Label" class="bg-zinc-900 border border-zinc-700 rounded px-2 py-1"/>
      <input value="${tb.icon_key||''}" onchange="updateNavTab(${idx},'icon_key',this.value)" placeholder="Icon Key" class="bg-zinc-900 border border-zinc-700 rounded px-2 py-1"/>
      <input value="${tb.badge_text||''}" onchange="updateNavTab(${idx},'badge_text',this.value)" placeholder="Badge" class="bg-zinc-900 border border-zinc-700 rounded px-2 py-1"/>
      <label class="flex items-center gap-1 justify-end"><input type="checkbox" ${tb.visible!==false?'checked':''} onchange="updateNavTab(${idx},'visible',this.checked)"/> Visible</label>
    </div>`).join('');
}

function updateNavTab(idx, key, val) {
  currentManifest.navigation.tabs[idx][key] = val;
  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderDesignPhonePreview();
}

function syncDesignFromInputs() {
  if (!currentManifest) return;
  currentManifest.design_version = ds_version.value.trim() || '1.0.0';
  currentManifest.release_notes = ds_notes.value;
  currentManifest.tokens.colors.mode = ds_mode.value;
  currentManifest.tokens.colors.background = ds_bg.value;
  currentManifest.tokens.colors.surface = ds_surface.value;
  currentManifest.tokens.colors.primary = ds_primary.value;
  currentManifest.tokens.colors.accent = ds_accent.value;
  currentManifest.tokens.colors.text_primary = ds_text.value;
  currentManifest.tokens.colors.border = ds_border.value;
  currentManifest.tokens.typography.display_font = ds_disp_font.value;
  currentManifest.tokens.typography.body_font = ds_body_font.value;
  currentManifest.tokens.radii.card_dp = parseInt(ds_card_radius.value || '6', 10);
  currentManifest.tokens.motion.intensity = ds_motion.value;

  currentManifest.navigation.top_bar_style = ds_top_style.value;
  currentManifest.navigation.top_bar_title = ds_top_title.value;
  currentManifest.navigation.top_bar_subtitle = ds_top_subtitle.value;
  currentManifest.navigation.bottom_bar_variant = ds_nav_variant.value;

  currentManifest.components.button.primary_variant = ds_btn_variant.value;
  currentManifest.components.button.corner_radius_dp = parseInt(ds_btn_radius.value || '4', 10);
  currentManifest.components.search_bar.variant = ds_search_variant.value;
  currentManifest.components.search_bar.placeholder = ds_search_ph.value;
  currentManifest.components.product_card.variant = ds_card_variant.value;
  currentManifest.components.product_card.image_aspect_ratio = parseFloat(ds_card_aspect.value || '0.78');
  currentManifest.components.category_strip.variant = ds_cat_variant.value;
  currentManifest.components.states.empty_title = ds_empty_title.value;

  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderDesignPhonePreview();
}

function renderPageComposer() {
  const pageKey = dsPageSelect.value || 'home';
  const page = currentManifest?.pages?.[pageKey];
  if (!page) return;
  dsPageHeroH.value = page.hero_height_dp ?? 420;
  dsPageCols.value = page.grid_columns ?? 2;
  const sections = (page.sections || []).slice().sort((a,b) => (a.order||0) - (b.order||0));
  page.sections = sections;
  document.getElementById('dsSectionsList').innerHTML = sections.map((sec, idx) => `
    <div class="flex flex-wrap items-center gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-2">
      <span class="w-6 text-center mono text-amber-400">${idx+1}</span>
      <span class="font-semibold w-32 truncate">${sec.id}</span>
      <input value="${sec.title||''}" onchange="updateSection(${idx},'title',this.value)" placeholder="Title" class="flex-1 min-w-[120px] bg-zinc-900 border border-zinc-700 rounded px-2 py-1"/>
      <input value="${sec.layout_variant||''}" onchange="updateSection(${idx},'layout_variant',this.value)" placeholder="Variant" class="w-28 bg-zinc-900 border border-zinc-700 rounded px-2 py-1"/>
      <label class="flex items-center gap-1"><input type="checkbox" ${sec.visible!==false?'checked':''} onchange="updateSection(${idx},'visible',this.checked)"/> Show</label>
      <button onclick="moveSection(${idx},-1)" class="px-2 py-1 rounded bg-zinc-800">&uarr;</button>
      <button onclick="moveSection(${idx},1)" class="px-2 py-1 rounded bg-zinc-800">&darr;</button>
    </div>`).join('');
  renderDesignPhonePreview();
}

function updatePageMeta() {
  const page = currentManifest.pages[dsPageSelect.value];
  if (!page) return;
  page.hero_height_dp = parseInt(dsPageHeroH.value || '420', 10);
  page.grid_columns = parseInt(dsPageCols.value || '2', 10);
  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderDesignPhonePreview();
}

function updateSection(idx, key, val) {
  const page = currentManifest.pages[dsPageSelect.value];
  page.sections[idx][key] = val;
  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderDesignPhonePreview();
}

function moveSection(idx, delta) {
  const page = currentManifest.pages[dsPageSelect.value];
  const next = idx + delta;
  if (next < 0 || next >= page.sections.length) return;
  const tmp = page.sections[idx];
  page.sections[idx] = page.sections[next];
  page.sections[next] = tmp;
  page.sections.forEach((s, i) => s.order = i + 1);
  dsRawJson.value = JSON.stringify(currentManifest, null, 2);
  renderPageComposer();
}

function applyRawJsonToEditor() {
  try {
    currentManifest = JSON.parse(dsRawJson.value);
    populateDesignInputs();
    toast('Applied JSON to studio editor');
  } catch (e) { alert('Invalid JSON: ' + e.message); }
}

function renderDesignPhonePreview() {
  if (!currentManifest) return;
  const c = currentManifest.tokens?.colors || {};
  const nav = currentManifest.navigation || {};
  const comp = currentManifest.components || {};
  const home = (currentManifest.pages?.home?.sections || []).filter(s => s.visible !== false).sort((a,b)=>(a.order||0)-(b.order||0));
  const bg = c.background || '#FFFFFF';
  const surf = c.surface || '#F7F7FA';
  const pri = c.primary || '#09090B';
  const acc = c.accent || '#C9A24B';
  const txt = c.text_primary || '#09090B';
  const brd = c.border || '#E4E4E7';
  const cardR = Math.min(20, currentManifest.tokens?.radii?.card_dp ?? 6);
  const btnR = Math.min(24, comp.button?.corner_radius_dp ?? 4);

  const sectionChips = home.slice(0, 5).map(s => {
    if (s.type === 'search_bar') {
      return `<div class="px-2.5 py-1.5 text-[10px] border truncate" style="border-color:${brd};background:${surf};border-radius:${Math.min(18,comp.search_bar?.corner_radius_dp??18)}px;color:${txt}99">${comp.search_bar?.placeholder||'Search…'}</div>`;
    }
    if (s.type === 'hero_carousel') {
      return `<div class="p-3 flex flex-col justify-end min-h-[115px]" style="background:${pri};color:${c.on_primary||'#FFF'};border-radius:${cardR}px">
        <div class="text-[8px] tracking-[0.2em]" style="color:${acc}">${s.eyebrow||'COVER'}</div>
        <div class="text-sm font-bold">${s.title||'Seasonal Campaign'}</div>
        <div class="mt-2 inline-block px-3 py-1 text-[9px] font-bold self-start" style="background:${acc};color:#111;border-radius:${btnR}px">${comp.button?.uppercase_labels ? (s.cta_label||'EXPLORE').toUpperCase() : (s.cta_label||'Explore')}</div>
      </div>`;
    }
    if (s.type === 'category_strip') {
      const tileR = comp.category_strip?.variant === 'circular_story' ? '999px' : cardR + 'px';
      return `<div>
        <div class="text-[9px] font-bold mb-1" style="color:${txt}">${s.title||'Categories'} (${comp.category_strip?.variant||'circular'})</div>
        <div class="grid grid-cols-4 gap-1.5">${['Dresses','Tailoring','Knit','Leather'].map(n=>`<div class="p-1.5 text-center text-[8px] border" style="background:${surf};border-color:${brd};border-radius:${tileR};color:${txt}">${n}</div>`).join('')}</div>
      </div>`;
    }
    return `<div class="p-2 border text-[9px] flex justify-between items-center" style="border-color:${brd};background:${surf};border-radius:${cardR}px;color:${txt}">
      <span class="font-semibold">${s.order}. ${s.title||s.id}</span><span style="color:${acc}">${s.layout_variant||''}</span>
    </div>`;
  }).join('');

  const tabsHtml = (nav.tabs || []).filter(t=>t.visible!==false).map((t,i)=>`
    <div class="text-center flex-1 ${i===0?'font-bold':'opacity-65'}">
      <div class="text-[8px]">[${t.icon_key}]</div>
      <div class="text-[9px]">${t.label}</div>
    </div>`).join('');

  const dockClass = nav.bottom_bar_variant === 'floating_dock' ? 'mx-3 mb-3 rounded-2xl shadow-lg' : 'border-t';
  const el = document.getElementById('designPhonePreview');
  el.style.background = bg;
  el.innerHTML = `
    <div>
      <div class="px-3 py-2.5 border-b flex items-center justify-between" style="border-color:${brd};color:${txt}">
        <div>
          <div class="text-xs font-bold tracking-widest">${nav.top_bar_title||'MELVORE'}</div>
          <div class="text-[8px] opacity-70">${nav.top_bar_subtitle||''}</div>
        </div>
        <span class="text-[9px] px-2 py-0.5 rounded" style="background:${acc};color:#111">v${currentManifest.design_version}</span>
      </div>
      <div class="p-3 space-y-2.5">${sectionChips}</div>
    </div>
    <div class="flex items-center justify-around py-2 px-2 ${dockClass}" style="background:${surf};border-color:${brd};color:${txt}">
      ${tabsHtml}
    </div>`;
}

async function saveDesignDraft() {
  syncDesignFromInputs();
  const res = await api('/admin/design.php', { method:'POST', body: JSON.stringify({
    action:'save_draft', slot: editingSlot, manifest: currentManifest, notes: ds_notes.value || 'Saved draft'
  })});
  toast('Saved ' + res.slot + ' draft (#' + res.release_id + ')');
  loadAll();
}

async function publishDesignSlot() {
  syncDesignFromInputs();
  const res = await api('/admin/design.php', { method:'POST', body: JSON.stringify({
    action:'publish', slot: editingSlot, manifest: currentManifest, version_tag: currentManifest.design_version, notes: ds_notes.value || 'Published from Studio'
  })});
  toast('Published ' + res.slot + ' v' + res.design_version);
  loadAll();
}

async function confirmSwitchDesign(flag) {
  const label = flag === 1 ? 'LIVE (1)' : 'DEFAULT (0)';
  if (!confirm('Switch global customer storefront to ' + label + '? Existing Cart/Checkout flows stay intact.')) return;
  await api('/admin/design.php', { method:'POST', body: JSON.stringify({ action:'switch_active', active_design: flag })});
  toast('Storefront switched to ' + label);
  loadAll();
}

async function rollbackToDefault() {
  if (!confirm('Immediately roll back all mobile clients to stable Default (0)?')) return;
  await api('/admin/design.php', { method:'POST', body: JSON.stringify({ action:'rollback', release_id: 0 })});
  toast('Rolled back to Default (0)');
  loadAll();
}

function renderDesignReleases() {
  const rows = designWorkspace?.releases || [];
  document.getElementById('dsReleasesList').innerHTML = rows.map(r => `
    <div class="flex flex-wrap items-center justify-between gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-2">
      <div>
        <span class="mono text-amber-400">#${r.release_id}</span> · <strong>${r.slot.toUpperCase()}</strong> v${r.version_tag}
        <span class="ml-1 px-1.5 py-0.5 rounded bg-zinc-800 text-[10px]">${r.status}</span>
        <div class="text-[10px] text-zinc-400">${r.release_notes||''}</div>
      </div>
      <div class="flex gap-1.5">
        <button onclick="duplicateRelease(${r.release_id},'live')" class="px-2 py-1 rounded bg-zinc-800 hover:bg-zinc-700">Dup &rarr; Live</button>
        <button onclick="rollbackRelease(${r.release_id})" class="px-2 py-1 rounded bg-red-900/70 text-red-100">Restore</button>
      </div>
    </div>`).join('') || '<p class="text-zinc-500">No releases logged in DB yet.</p>';
}

async function duplicateRelease(id, slot) {
  await api('/admin/design.php', { method:'POST', body: JSON.stringify({ action:'duplicate', release_id:id, target_slot:slot })});
  toast('Duplicated release #' + id); loadAll();
}

async function rollbackRelease(id) {
  if (!confirm('Restore and publish release #' + id + '?')) return;
  await api('/admin/design.php', { method:'POST', body: JSON.stringify({ action:'rollback', release_id:id })});
  toast('Restored release #' + id); loadAll();
}

async function compareTwoReleases() {
  const l = cmpLeft.value, r = cmpRight.value;
  if (!l || !r) return alert('Enter two release IDs to compare.');
  const res = await api(`/admin/design.php?action=compare&left_id=${l}&right_id=${r}`);
  const out = document.getElementById('dsDiffOutput');
  out.textContent = JSON.stringify(res.sections, null, 2);
  out.classList.remove('hidden');
}

function renderDesignAssets() {
  const assets = designWorkspace?.assets || [];
  document.getElementById('dsAssetsList').innerHTML = assets.map(a => `
    <div class="flex items-center justify-between gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-2">
      <div class="min-w-0">
        <div class="font-semibold truncate">${a.asset_key} <span class="text-amber-400">(${a.asset_type})</span></div>
        <div class="text-[10px] text-zinc-400 truncate">${a.relative_path}</div>
      </div>
      <button onclick="copyAssetPath('${a.relative_path}')" class="px-2 py-1 rounded bg-zinc-800 text-[10px]">Copy Path</button>
    </div>`).join('');
}

function copyAssetPath(p) { navigator.clipboard?.writeText(p); toast('Copied: ' + p); }

async function uploadDesignAsset() {
  const f = dsAssetFile.files?.[0];
  if (!f) return alert('Select a file');
  const fd = new FormData();
  fd.append('file', f);
  fd.append('asset_type', dsAssetType.value);
  fd.append('asset_key', dsAssetKey.value.trim() || f.name);
  await api('/admin/design_assets.php', { method:'POST', body: fd });
  toast('Uploaded asset to design-system/assets'); loadAll();
}

// OCCASIONS & SIMULATOR CONTROLLER
function renderOccasionsList() {
  document.getElementById('occasionsList').innerHTML = occasionsList.map(o => `
    <button onclick='selectOccasion(${JSON.stringify(o).replace(/'/g,"&#39;")})' class="w-full text-left px-3 py-2 rounded-lg text-xs border ${currentOccasion?.slug===o.slug?'border-amber-500 bg-zinc-800':'border-zinc-800 bg-zinc-950'}">
      <div class="font-semibold">${o.name} <span class="text-amber-400">P:${o.priority}</span></div>
      <div class="text-[10px] text-zinc-400">${o.event_date || 'Custom'} · -${o.activation_days_before ?? 8}d · ${o.region_country}${o.region_state?'/'+o.region_state:''}</div>
    </button>`).join('');
}

function selectOccasion(o) {
  currentOccasion = o;
  o_slug.value = o.slug || ''; o_name.value = o.name || '';
  o_eyebrow.value = o.eyebrow || ''; o_headline.value = o.headline || ''; o_subtitle.value = o.subtitle || '';
  o_event_date.value = o.event_date || ''; o_days_before.value = o.activation_days_before ?? 8;
  o_tz.value = o.timezone || 'Asia/Kolkata'; o_priority.value = o.priority ?? 80;
  o_country.value = o.region_country || 'India'; o_state.value = o.region_state || '';
  o_accent.value = o.accent_color || '#E6B34B'; o_bg.value = o.background_color || '#1B1224';
  o_active.checked = Number(o.is_active) === 1 || o.is_active === true;
  recomputeOccasionWindowPreview();
  renderOccasionsList();
}

function newOccasion() {
  selectOccasion({ slug:'diwali-edit', name:'Diwali Edit', eyebrow:'FESTIVE ISSUE', headline:'Golden Lights', event_date:'2026-10-20', activation_days_before:8, timezone:'Asia/Kolkata', priority:95, region_country:'India', region_state:'', is_active:1 });
}

function recomputeOccasionWindowPreview() {
  const ed = o_event_date.value;
  const days = parseInt(o_days_before.value || '8', 10);
  if (!ed) { o_window_preview.textContent = 'Set Event Date to compute the 8-day-before IST activation window.'; return; }
  const d = new Date(ed + 'T00:00:00');
  d.setDate(d.getDate() - days);
  const startStr = d.toISOString().slice(0, 10) + ' 00:00:00';
  o_window_preview.textContent = `Auto Window (${o_tz.value}): ${startStr}  ->  ${ed} 23:59:59 (${days} days before event)`;
}

async function saveOccasion() {
  await api('/admin/occasions.php', { method:'POST', body: JSON.stringify({
    slug: o_slug.value.trim(), name: o_name.value.trim(), eyebrow: o_eyebrow.value,
    headline: o_headline.value, subtitle: o_subtitle.value,
    event_date: o_event_date.value || null, activation_days_before: parseInt(o_days_before.value||'8', 10),
    timezone: o_tz.value || 'Asia/Kolkata', priority: parseInt(o_priority.value||'50', 10),
    region_country: o_country.value || 'ALL', region_state: o_state.value || '',
    accent_color: o_accent.value, background_color: o_bg.value, is_active: o_active.checked ? 1 : 0
  })});
  toast('Occasion saved with computed IST window!'); loadAll();
}

function runSimPreset(dt) { sim_dt.value = dt; runOccasionSimulation(); }

async function runOccasionSimulation() {
  const res = await api('/admin/occasion_preview.php', { method:'POST', body: JSON.stringify({
    simulated_datetime: sim_dt.value.trim(), country: sim_country.value.trim(),
    state: sim_state.value.trim(), world_slug: sim_world.value
  })});
  const win = res.active_occasion;
  document.getElementById('simOutput').innerHTML = `
    <div class="p-3 rounded-xl border ${win?'border-emerald-500 bg-emerald-950/30':'border-zinc-700 bg-zinc-950'}">
      <div class="text-[10px] uppercase tracking-wider text-zinc-400">Winner at ${res.simulated_input.datetime} IST</div>
      <div class="text-sm font-bold mt-0.5">${win ? win.name + ' (' + win.slug + ')' : 'None Active — Normal Storefront Design'}</div>
      ${res.blended_preview?.world_template ? `<div class="text-[11px] text-amber-300 mt-1">Blended Inside World: ${res.blended_preview.world_slug} (template preserved: ${res.blended_preview.world_template})</div>` : ''}
    </div>
    ${(res.evaluated||[]).map(e => `
      <div class="p-2 rounded border border-zinc-800 bg-zinc-950 flex justify-between gap-2 text-[11px]">
        <div><span class="font-semibold">${e.name}</span><div class="text-[10px] text-zinc-400">${e.reason}</div></div>
        <span class="${e.matched?'text-emerald-400':'text-zinc-500'} font-bold">${e.matched?'ACTIVE':'OFF'}</span>
      </div>`).join('')}`;
}

function renderWorldsList() {
  document.getElementById('worldsList').innerHTML = worlds.map(w => `
    <button onclick='selectWorld(${JSON.stringify(w).replace(/'/g,"&#39;")})' class="w-full text-left px-3 py-2 rounded-lg text-xs border ${currentWorld?.slug===w.slug?'border-amber-500 bg-zinc-800':'border-zinc-800 bg-zinc-950'}">
      <div class="font-semibold">${w.name}</div>
      <div class="text-zinc-400">${w.slug} · ${w.button_style||''}</div>
    </button>`).join('');
}

function selectWorld(w) {
  currentWorld = w;
  w_slug.value = w.slug || ''; w_name.value = w.name || ''; w_tagline.value = w.tagline || '';
  w_bg.value = w.background_color || '#0F0A1F'; w_primary.value = w.primary_color || '#FFFFFF'; w_accent.value = w.accent_color || '#22D3EE';
  w_font.value = w.font_style || 'editorial_serif'; w_button.value = w.button_style || 'editorial_pill'; w_anim.value = w.animation_key || 'smooth_reveal';
  w_audio_url.value = w.audio_url || ''; w_audio_vol.value = w.audio_volume || 0.45; w_audio_loop.checked = w.audio_loop !== false;
  w_promo_url.value = w.promo_video_url || ''; w_poster_url.value = w.promo_video_poster_url || ''; w_promo_sponsored.checked = !!w.promo_video_is_sponsored;
  w_runway_json.value = w.runway_config ? JSON.stringify(w.runway_config, null, 2) : '';
  renderWorldsList(); renderWorldPreview();
}

function newWorld() { selectWorld({ slug:'new-world', name:'New Fashion World', background_color:'#12121A', primary_color:'#FFFFFF', accent_color:'#D4AF37', font_style:'editorial_serif', button_style:'coquette_ribbon', audio_loop:true, audio_volume:0.45 }); }

function renderWorldPreview() {
  const bg = w_bg.value || '#12121A', acc = w_accent.value || '#22D3EE', pri = w_primary.value || '#FFFFFF';
  document.getElementById('worldPreview').style.background = bg;
  document.getElementById('worldPreview').innerHTML = `
    <div class="flex justify-between items-center text-[10px]" style="color:${pri}">
      <span class="px-2 py-1 rounded-full border" style="border-color:${acc}">&larr; Exit</span>
      <span class="px-2 py-1 rounded-full border" style="border-color:${acc}">Sound</span>
    </div>
    <div class="pt-4">
      <div class="text-[10px] tracking-[0.2em] uppercase" style="color:${acc}">${w_font.value}</div>
      <div class="text-xl font-bold mt-1" style="color:${pri}">${w_name.value || 'World Name'}</div>
      <p class="text-xs opacity-75 mt-1" style="color:${pri}">${w_tagline.value || ''}</p>
    </div>
    <div class="rounded-xl p-3 border text-[11px]" style="border-color:${acc}44;color:${pri}">
      <div class="font-semibold" style="color:${acc}">Button Style: ${w_button.value}</div>
      <div class="mt-2 inline-block px-4 py-2 rounded font-bold text-black" style="background:${acc}">Explore Capsule &rarr;</div>
    </div>
    ${w_runway_json.value.trim()?`<div class="rounded-xl p-3 border text-[10px]" style="border-color:${acc};color:${pri}">Animated Pixel Runway Section Enabled</div>`:''}
  `;
}

async function saveWorld() {
  let runway = null;
  if (w_runway_json.value.trim()) {
    try { runway = JSON.parse(w_runway_json.value); } catch (e) { return alert('Invalid Runway JSON: ' + e.message); }
  }
  await api('/admin/worlds.php', { method: 'POST', body: JSON.stringify({
    slug: w_slug.value.trim(), name: w_name.value.trim(), tagline: w_tagline.value,
    background_color: w_bg.value, primary_color: w_primary.value, accent_color: w_accent.value,
    font_style: w_font.value, button_style: w_button.value, animation_key: w_anim.value,
    audio_url: w_audio_url.value, audio_volume: parseFloat(w_audio_vol.value||'0.45'), audio_loop: w_audio_loop.checked,
    promo_video_url: w_promo_url.value, promo_video_poster_url: w_poster_url.value, promo_video_is_sponsored: w_promo_sponsored.checked,
    runway_config: runway, is_active: true
  })});
  toast('World saved!'); loadAll();
}

async function uploadAsset(input, kind, folder, targetInputId) {
  const file = input.files?.[0]; if (!file) return;
  const fd = new FormData(); fd.append('file', file); fd.append('kind', kind); fd.append('folder', folder);
  try {
    const res = await api('/uploads/upload.php', { method: 'POST', body: fd });
    document.getElementById(targetInputId).value = res.media_url || res.relative_path;
    toast('Uploaded ' + kind);
  } catch (e) { alert('Upload failed: ' + e.message); }
}

function renderLuxeList() {
  document.getElementById('luxeList').innerHTML = luxePacks.map(p => `
    <button onclick='selectLuxe(${JSON.stringify(p).replace(/'/g,"&#39;")})' class="w-full text-left px-3 py-2 rounded-lg text-xs border ${currentLuxe?.pack_id===p.pack_id?'border-amber-500 bg-zinc-800':'border-zinc-800 bg-zinc-950'}">
      <div class="font-semibold">${p.title}</div>
      <div class="text-zinc-400">${p.status} · ${(p.items||[]).length} items</div>
    </button>`).join('');
}

function selectLuxe(p) {
  currentLuxe = JSON.parse(JSON.stringify(p));
  l_slug.value = p.slug || ''; l_title.value = p.title || ''; l_brand.value = p.brand_name || 'MELVORE LUXE';
  l_layout.value = p.layout_type || 'editorial_grid'; l_subtitle.value = p.subtitle || ''; l_story.value = p.story || '';
  l_hero.value = p.hero_image_url || ''; l_status.value = p.status || 'published'; l_accent.value = p.accent_color || '#D4AF37';
  l_is_hero.checked = !!p.is_hero; l_is_sponsored.checked = !!p.is_sponsored;
  renderLuxeItems(); renderLuxeList(); renderLuxePreview();
}

function newLuxePack() {
  selectLuxe({ pack_id: 0, slug: 'new-luxe-drop', title: 'New Luxe Capsule', brand_name: 'MELVORE LUXE', status: 'draft', accent_color: '#D4AF37', items: [] });
}

function renderLuxeItems() {
  const items = currentLuxe?.items || [];
  document.getElementById('luxeItemsContainer').innerHTML = items.map((it, idx) => `
    <div class="flex items-center gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-2 text-xs">
      <img src="${it.image_url||''}" class="w-9 h-9 rounded object-cover bg-zinc-800"/>
      <div class="flex-1 min-w-0">
        <div class="truncate font-medium">${it.title || ('Product #' + it.product_id)}</div>
        <div class="text-[10px] text-zinc-400">ID ${it.product_id}</div>
      </div>
      <button onclick="removeLuxeItem(${idx})" class="text-red-400 px-2">Remove</button>
    </div>`).join('') || '<p class="text-xs text-zinc-500">No curated products assigned yet.</p>';
  renderLuxePreview();
}

function removeLuxeItem(idx) { currentLuxe.items.splice(idx, 1); renderLuxeItems(); }

function addProductToLuxe(prod) {
  if (!currentLuxe) return;
  currentLuxe.items = currentLuxe.items || [];
  if (currentLuxe.items.some(it => Number(it.product_id) === Number(prod.product_id))) {
    return toast('Product already inside collection');
  }
  currentLuxe.items.push({ item_type: 'product', product_id: prod.product_id, title: prod.name, image_url: prod.primary_image, product: prod });
  renderLuxeItems();
}

let searchTimer = null;
function searchCatalog() {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    const q = document.getElementById('prodSearchInput').value.trim();
    const res = await api('/admin/catalog_search.php?q=' + encodeURIComponent(q));
    document.getElementById('prodSearchResults').innerHTML = (res.products || []).map(pr => `
      <div class="flex items-center justify-between gap-2 bg-zinc-950 border border-zinc-800 rounded px-2 py-1.5 text-xs">
        <div class="flex items-center gap-2 min-w-0">
          <img src="${pr.primary_image||''}" class="w-7 h-7 rounded object-cover bg-zinc-800"/>
          <span class="truncate">${pr.name}</span>
        </div>
        <button onclick='addProductToLuxe(${JSON.stringify(pr).replace(/'/g,"&#39;")})' class="px-2 py-0.5 rounded bg-amber-500 text-zinc-950 font-semibold">+ Add</button>
      </div>`).join('');
  }, 250);
}

function renderLuxePreview() {
  const items = currentLuxe?.items || [];
  document.getElementById('luxePreview').innerHTML = `
    <div class="text-[10px] tracking-[0.2em]" style="color:${l_accent.value||'#D4AF37'}">${l_brand.value||'MELVORE LUXE'}</div>
    <div class="text-lg font-bold text-white">${l_title.value||'Collection Title'}</div>
    <p class="text-[11px] text-zinc-400">${l_subtitle.value||''}</p>
    <div class="grid grid-cols-2 gap-2 pt-2">
      ${items.slice(0,4).map(it => `
        <div class="rounded-lg bg-zinc-900 border border-zinc-800 overflow-hidden">
          <img src="${it.product?.primary_image || it.image_url || ''}" class="w-full h-24 object-cover bg-zinc-800"/>
          <div class="p-1.5 text-[10px] truncate text-white">${it.title||''}</div>
        </div>`).join('')}
    </div>`;
}

async function saveLuxePack() {
  await api('/admin/elite.php', { method: 'POST', body: JSON.stringify({
    pack_id: currentLuxe?.pack_id || 0, slug: l_slug.value.trim(), title: l_title.value.trim(),
    brand_name: l_brand.value.trim(), layout_type: l_layout.value, subtitle: l_subtitle.value, story: l_story.value,
    hero_image_url: l_hero.value, status: l_status.value, accent_color: l_accent.value,
    is_hero: l_is_hero.checked, is_sponsored: l_is_sponsored.checked,
    items: currentLuxe?.items || []
  })});
  toast('Luxe collection saved!'); loadAll();
}

async function duplicateLuxePack() {
  if (!currentLuxe?.pack_id) return;
  await api('/admin/elite.php', { method: 'POST', body: JSON.stringify({ action: 'duplicate_pack', pack_id: currentLuxe.pack_id })});
  toast('Collection duplicated'); loadAll();
}

async function archiveLuxePack() {
  if (!currentLuxe?.pack_id) return;
  await api('/admin/elite.php?pack_id=' + currentLuxe.pack_id, { method: 'DELETE' });
  toast('Collection archived'); loadAll();
}

function renderCatsList() {
  document.getElementById('catsList').innerHTML = categories.map(c => `
    <button onclick='selectCat(${JSON.stringify(c).replace(/'/g,"&#39;")})' class="w-full text-left px-3 py-2 rounded-lg text-xs border ${currentCat?.category_id===c.category_id?'border-amber-500 bg-zinc-800':'border-zinc-800 bg-zinc-950'}">
      <div class="font-semibold">${c.name}</div>
      <div class="text-zinc-400">${c.tile_span} · ${c.motif_key}</div>
    </button>`).join('');
}

function selectCat(c) {
  currentCat = c;
  c_badge.value = c.editorial_badge || ''; c_subtitle.value = c.editorial_subtitle || '';
  c_motif.value = c.motif_key || 'editorial_arch'; c_span.value = c.tile_span || 'regular';
  c_accent.value = c.accent_hex || '#141414'; c_bg.value = c.bg_hex || '#F8F7F4';
  c_order.value = c.display_order || 0; c_active.checked = c.is_active !== false;
  renderCatsList(); renderCatPreview();
}

function renderCatPreview() {
  if (!currentCat) return;
  document.getElementById('catPreview').style.background = c_bg.value || '#F8F7F4';
  document.getElementById('catPreview').innerHTML = `
    <div class="text-[10px] font-bold tracking-[0.2em]" style="color:${c_accent.value||'#141414'}">${c_badge.value||'CURATOR INDEX'}</div>
    <div class="text-xl font-bold mt-1">${currentCat.name}</div>
    <p class="text-xs opacity-70 mt-1">${c_subtitle.value||''}</p>
    <div class="mt-3 text-[11px] font-semibold" style="color:${c_accent.value||'#141414'}">Explore ${c_span.value} &rarr;</div>`;
}

async function saveCategoryConfig() {
  if (!currentCat) return;
  await api('/admin/categories.php', { method: 'POST', body: JSON.stringify({
    category_id: currentCat.category_id, editorial_badge: c_badge.value, editorial_subtitle: c_subtitle.value,
    motif_key: c_motif.value, tile_span: c_span.value, accent_hex: c_accent.value, bg_hex: c_bg.value,
    display_order: parseInt(c_order.value||'0', 10), is_active: c_active.checked
  })});
  toast('Category editorial showcase updated!'); loadAll();
}

initAuth();
</script>
</body>
</html>
