<script lang="ts">
	import { page } from '$app/stores';
	import Navbar from 'flowbite-svelte/Navbar.svelte';
	import NavBrand from 'flowbite-svelte/NavBrand.svelte';
	import NavLi from 'flowbite-svelte/NavLi.svelte';
	import NavUl from 'flowbite-svelte/NavUl.svelte';
	import NavHamburger from 'flowbite-svelte/NavHamburger.svelte';
	import DarkMode from 'flowbite-svelte/DarkMode.svelte';
	import LogoutModal from './LogoutModal.svelte';
	import { loggedInStatusStore, userIdStore } from '$lib/stores';
	let activeClass = 'text-primary dark:text-dark-primary font-bold';
	let classProp = '';
	export { classProp as class };
	$: activeUrl = $page.url.pathname;
	$: myCirclesHref = `/MyCircles/${$userIdStore}`;
	let showLogoutModal = false;
</script>

<header>
	<Navbar class={classProp}>
		<NavBrand href="/">
			<span class="text-primary dark:text-dark-primary font-extrabold">EXsplit</span>
		</NavBrand>
		<NavHamburger />
		<NavUl {activeUrl} {activeClass}>
			<NavLi href="/">Home</NavLi>
			{#if $loggedInStatusStore}
				<NavLi href={myCirclesHref}>My Circles</NavLi>
				<NavLi on:click={() => (showLogoutModal = true)}>Logout</NavLi>
			{:else}
				<NavLi href="/register">Register</NavLi>
				<NavLi href="/login">Login</NavLi>
			{/if}
		</NavUl>
		<DarkMode size="lg" />
	</Navbar>
	<LogoutModal bind:show={showLogoutModal} />
</header>
