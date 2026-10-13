<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/" aria-label="Northstar Inventory home">
        <span class="brand-mark" aria-hidden="true">N</span>
        <span>northstar<span class="brand-light"> / inventory</span></span>
      </RouterLink>
      <div class="topbar-meta"><span class="status-dot"></span> Workspace</div>
    </header>
    <RouterView />
  </div>
</template>
