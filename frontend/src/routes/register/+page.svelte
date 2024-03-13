<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	import Button from 'flowbite-svelte/Button.svelte';
	import PasswordInput from '../../components/forms/PasswordInput.svelte';
	import FormComponent from '../../components/forms/FormComponent.svelte';
	import EmailInput from '../../components/forms/EmailInput.svelte';
	import H1 from '../../components/shared/H1.svelte';
</script>

<header class="header-bg mb-20 grid grid-cols-1 place-items-center text-center">
	<H1>Register</H1>
</header>

<FormComponent>
	<P align="center" class="text-accent dark:text-dark-accent pb-8 text-xl font-bold">
		Get started with a free account.
	</P>
	<div class="mb-10 self-stretch">
		<EmailInput />
	</div>
	<div class="mb-10 self-stretch">
		<PasswordInput />
	</div>
	<div class="mb-10 self-stretch">
		<PasswordInput labelText="Confirm password" />
	</div>
	<Button
		type="submit"
		class=" bg-primary dark:bg-dark-primary hover:bg-accent dark:hover:bg-dark-accent self-stretch text-lg"
	>
		Register
	</Button>
</FormComponent>
