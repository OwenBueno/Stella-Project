import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type HabitDocument = HydratedDocument<Habit>;

@Schema({ ...stellaSchemaOptions, collection: 'Habit' })
export class Habit {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  name!: string;

  @Prop({ required: true })
  sortOrder!: number;

  @Prop({ default: true })
  active!: boolean;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date, default: null })
  deletedAt!: Date | null;
}

export const HabitSchema = SchemaFactory.createForClass(Habit);
HabitSchema.index({ updatedAt: 1 });
