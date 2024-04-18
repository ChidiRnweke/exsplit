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
	<P align="center" class="pb-8 text-xl font-bold text-accent dark:text-dark-accent">
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
		class=" self-stretch bg-primary text-lg hover:bg-accent dark:bg-dark-primary dark:hover:bg-dark-accent"
	>
		Register
	</Button>
</FormComponent>
