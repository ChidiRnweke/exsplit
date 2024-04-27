<script lang="ts">
	import type { PageData } from './$types';
	import Card from 'flowbite-svelte/Card.svelte';
	import Button from 'flowbite-svelte/Button.svelte';
	import Paragraph from '../../../components/shared/Paragraph.svelte';
	export let data: PageData;
	let numCircles = data.circles.length;
</script>

{#if numCircles === 0}
	<h3>You don't have any circles yet. You can make them here.</h3>
	<Button />
{:else}
	{#each data.circles as circle}
		<Card>
			<h5>
				{circle.circleName}
			</h5>
			<Paragraph>
				{circle.description}
			</Paragraph>
		</Card>
	{/each}
{/if}
