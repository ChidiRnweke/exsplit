<script>
	import Input from 'flowbite-svelte/Input.svelte';
	import Label from 'flowbite-svelte/Label.svelte';
	import EmailOutline from '~icons/mdi/email-outline.svelte';
</script>

<Label for="email" class="text-secondary dark:text-dark-secondary mb-2 text-xl font-extrabold">
	Email address
</Label>
<Input type="email" id="email" placeholder="john.doe@company.com" required autocomplete="email">
	<EmailOutline slot="left" />
</Input>
