<script lang="ts">
	import P from 'flowbite-svelte/P.svelte';
	import Heading from 'flowbite-svelte/Heading.svelte';
	import ImagePlaceholder from 'flowbite-svelte/ImagePlaceholder.svelte';
	export let title: string;
	export let paragraph: string;
	import type { SvelteComponent } from 'svelte';
	export let IconComponent: new (...args: any) => SvelteComponent;
	export let image: typeof ImagePlaceholder = ImagePlaceholder;
	export let tagLine: string;
</script>

<section class="mt-12">
	<div class="gap-10 md:grid md:grid-cols-2">
		<div class="grid grid-cols-1">
			<Heading tag="h3" class="font-extrabold text-secondary dark:text-dark-secondary"
				>{title}
			</Heading>

			<P class="mb-6 text-lg text-accent dark:text-dark-accent ">{tagLine}</P>
			<svelte:component
				this={IconComponent}
				class="relative mb-3 h-auto w-20 place-self-center rounded-lg bg-background-alt p-6 text-accent dark:bg-dark-background-alt dark:text-dark-accent  lg:place-self-start"
			/>
			<P class="text-foreground dark:text-dark-foreground ">{paragraph}</P>
		</div>
		<svelte:component this={image} />
	</div>
</section>
