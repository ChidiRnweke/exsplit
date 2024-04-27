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
	let confirmPassword = '';
	$: registerError = '';
	$: canSubmit = email && password == confirmPassword && password.length >= 8;

	const register = async () => {
		const { data, error } = await userClient.register({ email, password });
		if (error) {
			registerError = error.message;
		} else {
			storeLoginData(data.userId);
			goto(`MyCircles/${data.userId}`);
		}
	};
</script>

<header class="header-bg mb-20 grid grid-cols-1 place-items-center text-center">
	<H1>Register</H1>
</header>

<FormComponent>
	<P align="center" class="text-accent dark:text-dark-accent pb-8 text-xl font-bold">
		Get started with a free account.
	</P>
	<div class="mb-10 self-stretch">
		<EmailInput bind:email />
	</div>
	<div class="mb-10 self-stretch">
		<PasswordInput bind:password />
	</div>
	<div class="mb-10 self-stretch">
		<PasswordInput labelText="Confirm password" bind:password={confirmPassword} />
	</div>
	<Button
		type="submit"
		class=" bg-primary hover:bg-accent dark:bg-dark-primary dark:hover:bg-dark-accent self-stretch text-lg"
		on:click={register}
		disabled={!canSubmit}
	>
		Register
	</Button>
	{#if registerError}
		<P align="center" class=" text-error dark:text-dark-error pb-8 text-xl font-bold">
			{registerError}
		</P>
	{/if}
</FormComponent>
