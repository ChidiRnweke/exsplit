<script lang="ts">
	import Modal from 'flowbite-svelte/Modal.svelte';
	export let show = false;
	import Logout from '~icons/mdi/logout';
	import H3 from './H3.svelte';
	import Button from 'flowbite-svelte/Button.svelte';
	import { logout } from '$lib/stores';
	import { goto } from '$app/navigation';
</script>

<Modal bind:open={show} autoclose outsideclose size="md">
	<div class="pb-12 text-center">
		<Logout class="mx-auto mb-8 h-12 w-12" />
		<H3>Are you sure you want to log out?</H3>
		<Button
			class="bg-error hover:bg-secondary dark:bg-dark-error dark:hover:bg-dark-secondary ml-4 mr-4 mt-8 text-lg"
			on:click={() => logout(() => goto('/'))}
		>
			Yes, I'm sure.
		</Button>
		<Button
			class="bg-accent hover:bg-primary dark:bg-dark-accent dark:hover:bg-dark-primary ml-4 mr-4 mt-8 text-lg"
		>
			No, cancel.
		</Button>
	</div>
</Modal>
