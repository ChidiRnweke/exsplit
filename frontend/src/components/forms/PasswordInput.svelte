<script lang="ts">
	import Input from 'flowbite-svelte/Input.svelte';
	import Label from 'flowbite-svelte/Label.svelte';
	import EyeOutline from '~icons/mdi/eye-outline.svelte';
	import EyeOffOutline from '~icons/mdi/eye-off-outline.svelte';
	import FormTextBoxPassword from '~icons/mdi/form-textbox-password.svelte';
	export let password = '';
	let show = false;
	export let labelText = 'password';
	export let newPassword = true;
</script>

<Label for={labelText} class="text-secondary dark:text-dark-secondary mb-2 text-xl font-extrabold">
	{labelText}
</Label>
<Input
	type={show ? 'text' : 'password'}
	id={labelText}
	placeholder="•••••••••"
	required
	autocomplete={newPassword ? 'new-password' : 'current-password'}
	bind:value={password}
>
	<button slot="right" on:click|preventDefault={() => (show = !show)} class="pointer-events-auto">
		{#if show}
			<EyeOutline class="h-6 w-6" />
		{:else}
			<EyeOffOutline class="h-6 w-6" />
		{/if}
	</button>
	<FormTextBoxPassword slot="left" />
</Input>
