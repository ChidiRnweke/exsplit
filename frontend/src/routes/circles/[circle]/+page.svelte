<script lang="ts">
	import HeaderSection from '../../../components/shared/HeaderSection.svelte';
	import ImagePlaceholder from 'flowbite-svelte/ImagePlaceholder.svelte';
	import Card from 'flowbite-svelte/Card.svelte';
	let hCard = false;

	export let data;
</script>

<HeaderSection class="mb-20">Example circle</HeaderSection>

<div class="grid gap-4 md:grid-cols-2">
	{#each data.data as circle}
		<Card href={circle.id} size="lg" reverse={hCard}>
			<section class="grid grid-cols-2 gap-4">
				<ImagePlaceholder />
				<div>
					<h2 class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
						{circle.title}
					</h2>
					<p class="mb-3 font-normal leading-tight text-gray-700 dark:text-gray-400">
						{circle.description}
					</p>
				</div>
			</section>
		</Card>
	{/each}
</div>
