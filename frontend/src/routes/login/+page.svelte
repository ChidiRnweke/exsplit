<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	import Button from 'flowbite-svelte/Button.svelte';
	import PasswordInput from '../../components/forms/PasswordInput.svelte';
	import FormComponent from '../../components/forms/FormComponent.svelte';
	import EmailInput from '../../components/forms/EmailInput.svelte';
	import H1 from '../../components/shared/H1.svelte';
	import { userClient } from '$lib/api/clients/UserService';
	import { storeLoginData } from '$lib/stores';
	import { goto } from '$app/navigation';
	let email = '';
	let password = '';
	$: loginError = '';
	$: canSubmit = email && password.length >= 8;

	const login = async () => {
		const { data, error } = await userClient.login({ email, password });
		if (error) {
			loginError = error.message;
		} else {
			storeLoginData(data.userId);
			goto(`MyCircles/${data.userId}`);
		}
	};
</script>

<header class="header-bg mb-20 grid grid-cols-1 place-items-center text-center">
	<H1>Login</H1>
</header>

<FormComponent>
	<P align="center" class="text-accent dark:text-dark-accent pb-8 text-xl font-bold">
		Pick up where you left off.
	</P>
	<div class="mb-10 self-stretch">
		<EmailInput bind:email />
	</div>
	<div class="mb-10 self-stretch">
		<PasswordInput bind:password />
	</div>
	<Button
		type="submit"
		class=" bg-primary hover:bg-accent dark:bg-dark-primary dark:hover:bg-dark-accent self-stretch text-lg"
		on:click={login}
		disabled={!canSubmit}
	>
		Login
	</Button>
	{#if loginError}
		<P align="center" class="text-accent dark:text-dark-accent pb-8 text-xl font-bold">
			{loginError}
		</P>
	{/if}
</FormComponent>
