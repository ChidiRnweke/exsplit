<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	import Button from 'flowbite-svelte/Button.svelte';
	import PasswordInput from '../../components/forms/PasswordInput.svelte';
	import FormComponent from '../../components/forms/FormComponent.svelte';
	import EmailInput from '../../components/forms/EmailInput.svelte';
	import H1 from '../../components/shared/H1.svelte';
	import { userClient } from '$lib/api/clients/UserService';
	import { redirect } from '@sveltejs/kit';
	let email = '';
	let password = '';
	let confirmPassword = '';
	$: canSubmit = email && password == confirmPassword && password.length >= 8;

	const register = async () => {
		const { data, error } = await userClient.register({ email, password });
		if (error) {
			console.error(error);
		} else {
			redirect(303, '/login');
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
</FormComponent>
